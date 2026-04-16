package com.company.licenseengine.service;

import com.company.licenseengine.entity.ActionHistoryLog;
import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.entity.EmailResponse;
import com.company.licenseengine.repository.ActionHistoryLogRepository;
import com.company.licenseengine.repository.AuditAlertRepository;
import com.company.licenseengine.repository.EmailResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuditAlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditAlertService.class);
    
    @Autowired
    private AuditAlertRepository auditAlertRepository;
    
    @Autowired
    private ActionHistoryLogRepository actionHistoryLogRepository;
    
    @Autowired
    private EmailResponseRepository emailResponseRepository;
    
    @Autowired
    private MockApiService mockApiService;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.email.verification-base-url}")
    private String verificationBaseUrl;
    
    @Value("${app.email.response-deadline-days}")
    private int responseDeadlineDays;
    
    @Value("${app.email.response-deadline-minutes:0}")
    private int responseDeadlineMinutes;
    
    @Value("${app.email.extension-duration-days:30}")
    private int extensionDurationDays;
    
    @Value("${app.email.extension-duration-minutes:0}")
    private int extensionDurationMinutes;
    
    /**
     * Get all alerts visible on dashboard
     */
    public List<AuditAlert> getVisibleAlerts() {
        return auditAlertRepository.findVisibleAlerts();
    }
    
    /**
     * Get resolved alerts for history view
     */
    public List<AuditAlert> getResolvedAlerts() {
        return auditAlertRepository.findResolvedAlerts();
    }
    
    /**
     * Revoke a zombie license immediately
     */
    public boolean revokeZombieLicense(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getAlertType() != AuditAlert.AlertType.ZOMBIE) return false;
        
        // Call mock API to revoke license
        boolean success = mockApiService.revokeLicense(alert.getEmail(), alert.getVendorName());
        if (!success) return false;
        
        // Update alert status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.RESOLVED);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.REVOKE_ZOMBIE, justification, previousStatus, 
            AuditAlert.AlertStatus.RESOLVED);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Send verification email for low usage alert
     */
    public boolean sendVerificationEmail(Long alertId, String adminUsername) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getAlertType() != AuditAlert.AlertType.LOW_USAGE) return false;
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        alert.setVerificationToken(token);
        
        // Set response deadline (use minutes for testing if configured, otherwise days)
        LocalDateTime deadline;
        if (responseDeadlineMinutes > 0) {
            deadline = LocalDateTime.now().plusMinutes(responseDeadlineMinutes);
        } else {
            deadline = LocalDateTime.now().plusDays(responseDeadlineDays);
        }
        alert.setResponseDeadline(deadline);
        
        // Initialize reminder sent flag
        alert.setReminderSent(false);
        
        // Update status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.AWAITING_RESPONSE);
        auditAlertRepository.save(alert);
        
        // Send email
        String verificationUrl = verificationBaseUrl + "/" + token;
        boolean emailSent = emailService.sendVerificationEmail(
            alert.getEmail(), 
            alert.getEmployeeName(), 
            alert.getVendorName(), 
            verificationUrl, 
            alert.getResponseDeadline().toString()
        );
        
        if (emailSent) {
            // Log action
            ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
                ActionHistoryLog.ActionType.SEND_EMAIL, "Verification email sent", 
                previousStatus, AuditAlert.AlertStatus.AWAITING_RESPONSE);
            actionHistoryLogRepository.save(log);
        }
        
        return emailSent;
    }
    
    /**
     * Process user response to verification email
     */
    public boolean processUserResponse(String token, EmailResponse.ResponseType responseType, 
                                     String reason, Integer durationDays) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findByVerificationToken(token);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        
        // Create email response record
        EmailResponse response = new EmailResponse(alert, responseType, reason, durationDays);
        emailResponseRepository.save(response);
        
        if (responseType == EmailResponse.ResponseType.SURRENDER_LICENSE) {
            // User surrendered license - revoke immediately
            boolean success = mockApiService.revokeLicense(alert.getEmail(), alert.getVendorName());
            if (success) {
                alert.setStatus(AuditAlert.AlertStatus.RESOLVED);
            }
        } else {
            // User wants to keep license - mark for admin review
            alert.setStatus(AuditAlert.AlertStatus.READY_FOR_REVIEW);
        }
        
        auditAlertRepository.save(alert);
        return true;
    }
    
    /**
     * Admin approves extension request
     */
    public boolean approveExtension(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        EmailResponse response = alert.getEmailResponse();
        if (response == null) return false;
        
        // Set extension expiration (use minutes for testing if configured, otherwise days)
        LocalDateTime extensionExpiration;
        if (extensionDurationMinutes > 0) {
            // Testing mode: use minutes instead of days
            int requestedMinutes = response.getRequestedDurationDays(); // Treat "days" as minutes in test mode
            extensionExpiration = LocalDateTime.now()
                .plusMinutes(extensionDurationMinutes); // Use configured test duration (4 minutes)
            logger.info("TESTING MODE: Extension set for {} minutes (expires at: {})", 
                extensionDurationMinutes, extensionExpiration);
        } else {
            // Production mode: use days
            extensionExpiration = LocalDateTime.now()
                .plusDays(response.getRequestedDurationDays())
                .plusDays(2); // Buffer days
        }
        alert.setExtensionExpiration(extensionExpiration);
        
        // Update status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.APPROVED_EXTENSION);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.APPROVE_EXTENSION, justification, 
            previousStatus, AuditAlert.AlertStatus.APPROVED_EXTENSION);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Admin rejects extension and revokes license
     */
    public boolean rejectAndRevoke(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        
        // Revoke license
        boolean success = mockApiService.revokeLicense(alert.getEmail(), alert.getVendorName());
        if (!success) return false;
        
        // Update status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.RESOLVED);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.REJECT_AND_REVOKE, justification, 
            previousStatus, AuditAlert.AlertStatus.RESOLVED);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Extend deadline for overdue response
     */
    public boolean extendDeadline(Long alertId, String adminUsername, int additionalDays, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        
        // Extend deadline (use minutes for testing if configured, otherwise days)
        LocalDateTime newDeadline;
        if (responseDeadlineMinutes > 0) {
            // For testing mode, extend by minutes
            newDeadline = LocalDateTime.now().plusMinutes(additionalDays); // additionalDays becomes additional minutes in test mode
        } else {
            // Normal mode, extend by days
            newDeadline = LocalDateTime.now().plusDays(additionalDays);
        }
        alert.setResponseDeadline(newDeadline);
        
        // Update status back to awaiting response
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.AWAITING_RESPONSE);
        auditAlertRepository.save(alert);
        
        // Send reminder email
        String verificationUrl = verificationBaseUrl + "/" + alert.getVerificationToken();
        emailService.sendReminderEmail(
            alert.getEmail(), 
            alert.getEmployeeName(), 
            alert.getVendorName(), 
            verificationUrl, 
            alert.getResponseDeadline().toString()
        );
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.EXTEND_DEADLINE, justification, 
            previousStatus, AuditAlert.AlertStatus.AWAITING_RESPONSE);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Revoke overdue license
     */
    public boolean revokeOverdueLicense(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getStatus() != AuditAlert.AlertStatus.RESPONSE_OVERDUE) return false;
        
        // Call mock API to revoke license
        boolean success = mockApiService.revokeLicense(alert.getEmail(), alert.getVendorName());
        if (!success) return false;
        
        // Update alert status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.RESOLVED);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.REVOKE_OVERDUE, justification, previousStatus, 
            AuditAlert.AlertStatus.RESOLVED);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Extend expired license again
     */
    public boolean extendExpiredLicense(Long alertId, String adminUsername, int extensionDays, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getStatus() != AuditAlert.AlertStatus.EXTENSION_EXPIRED) return false;
        
        // Set new extension expiration (use minutes for testing if configured, otherwise days)
        LocalDateTime newExtensionExpiration;
        if (extensionDurationMinutes > 0) {
            // Testing mode: treat extensionDays as minutes
            newExtensionExpiration = LocalDateTime.now().plusMinutes(extensionDurationMinutes);
            logger.info("TESTING MODE: New extension set for {} minutes (expires at: {})", 
                extensionDurationMinutes, newExtensionExpiration);
        } else {
            // Production mode: use days
            newExtensionExpiration = LocalDateTime.now().plusDays(extensionDays).plusDays(2); // Buffer days
        }
        alert.setExtensionExpiration(newExtensionExpiration);
        
        // Update status back to approved extension
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.APPROVED_EXTENSION);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.APPROVE_EXTENSION, justification, 
            previousStatus, AuditAlert.AlertStatus.APPROVED_EXTENSION);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Revoke expired license
     */
    public boolean revokeExpiredLicense(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getStatus() != AuditAlert.AlertStatus.EXTENSION_EXPIRED) return false;
        
        // Call mock API to revoke license
        boolean success = mockApiService.revokeLicense(alert.getEmail(), alert.getVendorName());
        if (!success) return false;
        
        // Update alert status
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.RESOLVED);
        auditAlertRepository.save(alert);
        
        // Log action
        ActionHistoryLog log = new ActionHistoryLog(alert, adminUsername, 
            ActionHistoryLog.ActionType.REVOKE_EXPIRED, justification, previousStatus, 
            AuditAlert.AlertStatus.RESOLVED);
        actionHistoryLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Get alert by ID
     */
    public Optional<AuditAlert> getAlertById(Long id) {
        return auditAlertRepository.findById(id);
    }
    
    /**
     * Get alert by verification token
     */
    public Optional<AuditAlert> getAlertByToken(String token) {
        return auditAlertRepository.findByVerificationToken(token);
    }
}