package com.company.licenseengine.service;

import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.repository.AuditAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    /**
     * Check for overdue responses every hour
     * Cron: "0 0 * * * *" = Every hour at minute 0
     */
    @Scheduled(cron = "${app.scheduling.deadline-check-cron}")
    public void checkOverdueResponses() {
        logger.info("Checking for overdue responses...");
        
        LocalDateTime now = LocalDateTime.now();
        List<AuditAlert> overdueAlerts = auditAlertRepository.findOverdueResponses(now);
        
        for (AuditAlert alert : overdueAlerts) {
            logger.info("Marking alert {} as overdue (deadline: {})", 
                alert.getId(), alert.getResponseDeadline());
            
            alert.setStatus(AuditAlert.AlertStatus.RESPONSE_OVERDUE);
            auditAlertRepository.save(alert);
        }
        
        if (!overdueAlerts.isEmpty()) {
            logger.info("Marked {} alerts as overdue", overdueAlerts.size());
        }
    }
    
    /**
     * Check for expired extensions every hour at 30 minutes
     * Cron: "0 30 * * * *" = Every hour at minute 30
     */
    @Scheduled(cron = "${app.scheduling.extension-check-cron}")
    public void checkExpiredExtensions() {
        logger.info("Checking for expired extensions...");
        
        LocalDateTime now = LocalDateTime.now();
        List<AuditAlert> expiredAlerts = auditAlertRepository.findExpiredExtensions(now);
        
        for (AuditAlert alert : expiredAlerts) {
            logger.info("Marking alert {} as extension expired (expiration: {})", 
                alert.getId(), alert.getExtensionExpiration());
            
            alert.setStatus(AuditAlert.AlertStatus.EXTENSION_EXPIRED);
            auditAlertRepository.save(alert);
        }
        
        if (!expiredAlerts.isEmpty()) {
            logger.info("Marked {} extensions as expired", expiredAlerts.size());
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