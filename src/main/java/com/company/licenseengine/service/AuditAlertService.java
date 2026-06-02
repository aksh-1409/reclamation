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

import java.math.BigDecimal;
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
    
    @Autowired
    private CostCalculationService costCalculationService;
    
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
     * Get all action history for audit trail
     */
    public List<ActionHistoryLog> getAllActionHistory() {
        try {
            // Try the JOIN FETCH query first
            List<ActionHistoryLog> actionHistory = actionHistoryLogRepository.findAllWithAuditAlertByOrderByCreatedAtDesc();
            logger.info("DEBUG: Retrieved {} action history records using JOIN FETCH", actionHistory.size());
            return actionHistory;
        } catch (Exception e) {
            logger.error("ERROR: JOIN FETCH query failed, falling back to simple query", e);
            // Fallback to simple query
            List<ActionHistoryLog> actionHistory = actionHistoryLogRepository.findAllByOrderByCreatedAtDesc();
            logger.info("DEBUG: Retrieved {} action history records using simple query", actionHistory.size());
            return actionHistory;
        }
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
        
        // Calculate and save cost savings
        BigDecimal costSaved = costCalculationService.calculateCostSavedForAlert(alert);
        alert.setCostSaved(costSaved);
        logger.info("DEBUG: Zombie license revoked - Monthly Cost: ${}, Cost saved: ${} for alert {}", 
            alert.getMonthlyCost(), costSaved, alertId);
        
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
        String verificationUrl = verificationBaseUrl + "/verify/" + token;
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
        
        // Check if response already exists - if so, update it instead of creating new
        EmailResponse response;
        if (alert.getEmailResponse() != null) {
            // Update existing response
            response = alert.getEmailResponse();
            response.setResponseType(responseType);
            response.setReason(reason);
            response.setRequestedDurationDays(durationDays);
            response.setRespondedAt(LocalDateTime.now());
            System.out.println("Updating existing response for alert ID: " + alert.getId());
        } else {
            // Create new email response record
            response = new EmailResponse(alert, responseType, reason, durationDays);
            System.out.println("Creating new response for alert ID: " + alert.getId());
        }
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
        
        // Set extension expiration (always use 4 minutes for testing, regardless of user request)
        LocalDateTime extensionExpiration;
        if (extensionDurationMinutes > 0) {
            // Testing mode: always use configured test duration (4 minutes) regardless of user request
            extensionExpiration = LocalDateTime.now().plusMinutes(extensionDurationMinutes);
            logger.info("TESTING MODE: Extension set for {} minutes (user requested {} days, but using test duration) (expires at: {})", 
                extensionDurationMinutes, response.getRequestedDurationDays(), extensionExpiration);
        } else {
            // Production mode: use user's requested days
            extensionExpiration = LocalDateTime.now().plusDays(response.getRequestedDurationDays());
            logger.info("PRODUCTION MODE: Extension set for {} days (expires at: {})", 
                response.getRequestedDurationDays(), extensionExpiration);
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
        
        // Calculate and save cost savings
        BigDecimal costSaved = costCalculationService.calculateCostSavedForAlert(alert);
        alert.setCostSaved(costSaved);
        logger.info("DEBUG: License rejected and revoked - Monthly Cost: ${}, Cost saved: ${} for alert {}", 
            alert.getMonthlyCost(), costSaved, alertId);
        
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
        String verificationUrl = verificationBaseUrl + "/verify/" + alert.getVerificationToken();
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
        
        // Set new extension expiration (always use 4 minutes for testing, regardless of user request)
        LocalDateTime newExtensionExpiration;
        if (extensionDurationMinutes > 0) {
            // Testing mode: always use configured test duration (4 minutes) regardless of user request
            newExtensionExpiration = LocalDateTime.now().plusMinutes(extensionDurationMinutes);
            logger.info("TESTING MODE: New extension set for {} minutes (user originally requested {} days, but using test duration) (expires at: {})", 
                extensionDurationMinutes, 
                (alert.getEmailResponse() != null ? alert.getEmailResponse().getRequestedDurationDays() : extensionDays), 
                newExtensionExpiration);
        } else {
            // Production mode: use days (prefer user's original request if available)
            if (alert.getEmailResponse() != null && alert.getEmailResponse().getRequestedDurationDays() != null) {
                newExtensionExpiration = LocalDateTime.now().plusDays(alert.getEmailResponse().getRequestedDurationDays());
                logger.info("PRODUCTION MODE: New extension set for {} days (user's original request) (expires at: {})", 
                    alert.getEmailResponse().getRequestedDurationDays(), newExtensionExpiration);
            } else {
                newExtensionExpiration = LocalDateTime.now().plusDays(extensionDays);
                logger.info("PRODUCTION MODE: New extension set for {} days (admin-specified) (expires at: {})", 
                    extensionDays, newExtensionExpiration);
            }
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
     * Send inquiry email for expired extension
     */
    public boolean sendInquiryEmailForExpiredExtension(Long alertId, String adminUsername, String justification) {
        Optional<AuditAlert> alertOpt = auditAlertRepository.findById(alertId);
        if (alertOpt.isEmpty()) return false;
        
        AuditAlert alert = alertOpt.get();
        if (alert.getStatus() != AuditAlert.AlertStatus.EXTENSION_EXPIRED) return false;
        
        // Generate new verification token (reuse existing or create new)
        if (alert.getVerificationToken() == null || alert.getVerificationToken().isEmpty()) {
            alert.setVerificationToken(UUID.randomUUID().toString());
        }
        
        // Set new response deadline (use minutes for testing if configured, otherwise days)
        LocalDateTime deadline;
        if (responseDeadlineMinutes > 0) {
            deadline = LocalDateTime.now().plusMinutes(responseDeadlineMinutes);
        } else {
            deadline = LocalDateTime.now().plusDays(responseDeadlineDays);
        }
        alert.setResponseDeadline(deadline);
        
        // Reset reminder sent flag for new inquiry
        alert.setReminderSent(false);
        
        // Update status to awaiting response
        AuditAlert.AlertStatus previousStatus = alert.getStatus();
        alert.setStatus(AuditAlert.AlertStatus.AWAITING_RESPONSE);
        auditAlertRepository.save(alert);
        
        // Send inquiry email
        String verificationUrl = verificationBaseUrl + "/verify/" + alert.getVerificationToken();
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
                ActionHistoryLog.ActionType.SEND_EMAIL, justification, 
                previousStatus, AuditAlert.AlertStatus.AWAITING_RESPONSE);
            actionHistoryLogRepository.save(log);
            
            logger.info("Inquiry email sent for expired extension - Alert ID: {}, Admin: {}", alertId, adminUsername);
        }
        
        return emailSent;
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