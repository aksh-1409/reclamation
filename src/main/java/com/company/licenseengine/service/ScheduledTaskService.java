package com.company.licenseengine.service;

import com.company.licenseengine.entity.ActionHistoryLog;
import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.repository.ActionHistoryLogRepository;
import com.company.licenseengine.repository.AuditAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    @Autowired
    private AuditAlertRepository auditAlertRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private ActionHistoryLogRepository actionHistoryLogRepository;
    
    @Value("${app.email.verification-base-url}")
    private String verificationBaseUrl;
    
    @Value("${app.email.reminder-before-minutes:60}")
    private int reminderBeforeMinutes;
    
    /**
     * Check for overdue responses every hour
     * Cron: "0 0 * * * *" = Every hour at minute 0
     */
    @Scheduled(cron = "${app.scheduling.deadline-check-cron}")
    public void checkOverdueResponses() {
        logger.info("=== CHECKING FOR OVERDUE RESPONSES (Testing Mode) ===");
        
        LocalDateTime now = LocalDateTime.now();
        logger.info("Current time: {}", now);
        
        // First, check for reminder emails that need to be sent
        checkAndSendReminderEmails(now);
        
        // Then, check for overdue responses
        List<AuditAlert> overdueAlerts = auditAlertRepository.findOverdueResponses(now);
        logger.info("Found {} alerts to check for overdue status", overdueAlerts.size());
        
        for (AuditAlert alert : overdueAlerts) {
            logger.info("Marking alert {} as overdue (deadline: {}, current: {})", 
                alert.getId(), alert.getResponseDeadline(), now);
            
            alert.setStatus(AuditAlert.AlertStatus.RESPONSE_OVERDUE);
            auditAlertRepository.save(alert);
        }
        
        if (!overdueAlerts.isEmpty()) {
            logger.info("*** MARKED {} ALERTS AS OVERDUE ***", overdueAlerts.size());
        } else {
            logger.info("No overdue alerts found at this time");
        }
    }
    
    /**
     * Check and send reminder emails for alerts approaching deadline
     */
    private void checkAndSendReminderEmails(LocalDateTime now) {
        logger.info("=== CHECKING FOR REMINDER EMAILS ===");
        
        LocalDateTime reminderTime = now.plusMinutes(reminderBeforeMinutes);
        logger.info("Looking for alerts with deadline between {} and {} that need reminders", now, reminderTime);
        
        List<AuditAlert> alertsNeedingReminder = auditAlertRepository.findAlertsNeedingReminder(now, reminderTime);
        logger.info("Found {} alerts that need reminder emails", alertsNeedingReminder.size());
        
        for (AuditAlert alert : alertsNeedingReminder) {
            logger.info("Sending reminder email for alert {} (deadline: {})", alert.getId(), alert.getResponseDeadline());
            
            // Send reminder email
            String verificationUrl = verificationBaseUrl + "/" + alert.getVerificationToken();
            boolean emailSent = emailService.sendReminderEmail(
                alert.getEmail(),
                alert.getEmployeeName(),
                alert.getVendorName(),
                verificationUrl,
                alert.getResponseDeadline().toString()
            );
            
            if (emailSent) {
                // Mark reminder as sent
                alert.setReminderSent(true);
                auditAlertRepository.save(alert);
                
                // Log the reminder action
                ActionHistoryLog log = new ActionHistoryLog(alert, "SYSTEM", 
                    ActionHistoryLog.ActionType.SEND_REMINDER, 
                    "Automatic reminder email sent before deadline", 
                    alert.getStatus(), alert.getStatus());
                actionHistoryLogRepository.save(log);
                
                logger.info("*** REMINDER EMAIL SENT for alert {} ***", alert.getId());
            } else {
                logger.error("Failed to send reminder email for alert {}", alert.getId());
            }
        }
        
        if (alertsNeedingReminder.isEmpty()) {
            logger.info("No reminder emails needed at this time");
        }
    }
    
    /**
     * Check for expired extensions every hour at 30 minutes
     * Cron: "0 30 * * * *" = Every hour at minute 30
     */
    @Scheduled(cron = "${app.scheduling.extension-check-cron}")
    public void checkExpiredExtensions() {
        logger.info("=== CHECKING FOR EXPIRED EXTENSIONS (Testing Mode) ===");
        
        LocalDateTime now = LocalDateTime.now();
        logger.info("Current time: {}", now);
        
        List<AuditAlert> expiredAlerts = auditAlertRepository.findExpiredExtensions(now);
        logger.info("Found {} extensions to check for expiration", expiredAlerts.size());
        
        for (AuditAlert alert : expiredAlerts) {
            logger.info("Marking alert {} as extension expired (expiration: {}, current: {})", 
                alert.getId(), alert.getExtensionExpiration(), now);
            
            alert.setStatus(AuditAlert.AlertStatus.EXTENSION_EXPIRED);
            auditAlertRepository.save(alert);
        }
        
        if (!expiredAlerts.isEmpty()) {
            logger.info("*** MARKED {} EXTENSIONS AS EXPIRED ***", expiredAlerts.size());
        } else {
            logger.info("No expired extensions found at this time");
        }
    }
    
    /**
     * Manual method to trigger deadline checks (for testing)
     */
    public int manualDeadlineCheck() {
        LocalDateTime now = LocalDateTime.now();
        List<AuditAlert> overdueAlerts = auditAlertRepository.findOverdueResponses(now);
        
        for (AuditAlert alert : overdueAlerts) {
            alert.setStatus(AuditAlert.AlertStatus.RESPONSE_OVERDUE);
            auditAlertRepository.save(alert);
        }
        
        return overdueAlerts.size();
    }
    
    /**
     * Manual method to trigger extension checks (for testing)
     */
    public int manualExtensionCheck() {
        LocalDateTime now = LocalDateTime.now();
        List<AuditAlert> expiredAlerts = auditAlertRepository.findExpiredExtensions(now);
        
        for (AuditAlert alert : expiredAlerts) {
            alert.setStatus(AuditAlert.AlertStatus.EXTENSION_EXPIRED);
            auditAlertRepository.save(alert);
        }
        
        return expiredAlerts.size();
    }
}