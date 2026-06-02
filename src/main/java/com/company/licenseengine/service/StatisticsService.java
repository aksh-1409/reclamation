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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    
    @Autowired
    private AuditAlertRepository auditAlertRepository;
    
    @Autowired
    private ActionHistoryLogRepository actionHistoryLogRepository;
    
    @Autowired
    private EmailResponseRepository emailResponseRepository;
    
    @Autowired
    private CostCalculationService costCalculationService;
    
    /**
     * Get comprehensive system statistics
     */
    @Cacheable(value = "systemStats", unless = "#result == null")
    public SystemStatistics getSystemStatistics() {
        logger.info("Calculating system statistics...");
        
        List<AuditAlert> allAlerts = auditAlertRepository.findAll();
        List<AuditAlert> resolvedAlerts = auditAlertRepository.findResolvedAlerts();
        List<EmailResponse> allResponses = emailResponseRepository.findAll();
        
        // Basic counts
        int totalAlertsProcessed = allAlerts.size();
        int totalResolved = resolvedAlerts.size();
        
        // Response rate calculation
        long alertsWithEmailSent = allAlerts.stream()
            .filter(alert -> alert.getAlertType() == AuditAlert.AlertType.LOW_USAGE)
            .filter(alert -> !alert.getStatus().equals(AuditAlert.AlertStatus.NEW))
            .count();
        
        double overallResponseRate = alertsWithEmailSent > 0 ? 
            (double) allResponses.size() / alertsWithEmailSent * 100 : 0.0;
        
        // Average response time calculation
        double averageResponseTimeHours = calculateAverageResponseTime();
        
        // Extension approval rate
        double extensionApprovalRate = calculateExtensionApprovalRate();
        
        // Revocation rate
        double revocationRate = totalAlertsProcessed > 0 ? 
            (double) totalResolved / totalAlertsProcessed * 100 : 0.0;
        
        // Cost savings
        BigDecimal totalCostSavings = costCalculationService.calculateTotalSavings();
        
        // Alerts by status
        Map<String, Integer> alertsByStatus = allAlerts.stream()
            .collect(Collectors.groupingBy(
                alert -> alert.getStatus().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Alerts by type
        Map<String, Integer> alertsByType = allAlerts.stream()
            .collect(Collectors.groupingBy(
                alert -> alert.getAlertType().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        SystemStatistics stats = new SystemStatistics(
            totalAlertsProcessed,
            Math.round(overallResponseRate * 100.0) / 100.0,
            Math.round(averageResponseTimeHours * 100.0) / 100.0,
            Math.round(extensionApprovalRate * 100.0) / 100.0,
            Math.round(revocationRate * 100.0) / 100.0,
            totalCostSavings,
            alertsByStatus,
            alertsByType
        );
        
        logger.info("System statistics calculated: {} total alerts, {}% response rate", 
            totalAlertsProcessed, stats.getOverallResponseRate());
        
        return stats;
    }
    
    /**
     * Get vendor-specific statistics
     */
    @Cacheable(value = "vendorStats", unless = "#result == null")
    public List<VendorStatistics> getVendorStatistics() {
        logger.info("Calculating vendor statistics...");
        
        List<AuditAlert> allAlerts = auditAlertRepository.findAll();
        Map<String, List<AuditAlert>> alertsByVendor = allAlerts.stream()
            .filter(alert -> alert.getVendorName() != null)
            .collect(Collectors.groupingBy(AuditAlert::getVendorName));
        
        List<VendorStatistics> vendorStats = new ArrayList<>();
        
        for (Map.Entry<String, List<AuditAlert>> entry : alertsByVendor.entrySet()) {
            String vendorName = entry.getKey();
            List<AuditAlert> vendorAlerts = entry.getValue();
            
            // Basic counts
            int totalAlerts = vendorAlerts.size();
            long zombieCount = vendorAlerts.stream()
                .filter(alert -> alert.getAlertType() == AuditAlert.AlertType.ZOMBIE)
                .count();
            long lowUsageCount = vendorAlerts.stream()
                .filter(alert -> alert.getAlertType() == AuditAlert.AlertType.LOW_USAGE)
                .count();
            
            // Response rate for this vendor
            long emailsSent = vendorAlerts.stream()
                .filter(alert -> alert.getAlertType() == AuditAlert.AlertType.LOW_USAGE)
                .filter(alert -> !alert.getStatus().equals(AuditAlert.AlertStatus.NEW))
                .count();
            
            long responsesReceived = vendorAlerts.stream()
                .filter(alert -> alert.getEmailResponse() != null)
                .count();
            
            double responseRate = emailsSent > 0 ? 
                (double) responsesReceived / emailsSent * 100 : 0.0;
            
            // Average response time for this vendor
            double averageResponseTime = calculateVendorAverageResponseTime(vendorName);
            
            // Cost savings for this vendor
            BigDecimal costSavings = vendorAlerts.stream()
                .filter(alert -> alert.getCostSaved() != null)
                .map(AuditAlert::getCostSaved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            VendorStatistics stats = new VendorStatistics(
                vendorName,
                totalAlerts,
                Math.round(responseRate * 100.0) / 100.0,
                Math.round(averageResponseTime * 100.0) / 100.0,
                costSavings,
                (int) zombieCount,
                (int) lowUsageCount
            );
            
            vendorStats.add(stats);
        }
        
        // Sort by total alerts descending
        vendorStats.sort((a, b) -> Integer.compare(b.getTotalAlerts(), a.getTotalAlerts()));
        
        logger.info("Vendor statistics calculated for {} vendors", vendorStats.size());
        return vendorStats;
    }
    
    /**
     * Get trend data for specified number of days
     */
    public List<TrendData> getTrendData(int days) {
        logger.info("Calculating trend data for {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<AuditAlert> alerts = auditAlertRepository.findAll().stream()
            .filter(alert -> alert.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        // Group alerts by date
        Map<String, List<AuditAlert>> alertsByDate = alerts.stream()
            .collect(Collectors.groupingBy(
                alert -> alert.getCreatedAt().toLocalDate().toString()
            ));
        
        List<TrendData> trends = new ArrayList<>();
        
        // Generate trend data for each day
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateKey = date.toLocalDate().toString();
            
            List<AuditAlert> dayAlerts = alertsByDate.getOrDefault(dateKey, new ArrayList<>());
            
            int alertsCreated = dayAlerts.size();
            long alertsResolved = dayAlerts.stream()
                .filter(alert -> alert.getStatus() == AuditAlert.AlertStatus.RESOLVED)
                .count();
            
            BigDecimal costSaved = dayAlerts.stream()
                .filter(alert -> alert.getCostSaved() != null)
                .map(AuditAlert::getCostSaved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate response rate for the day
            long emailsSent = dayAlerts.stream()
                .filter(alert -> alert.getAlertType() == AuditAlert.AlertType.LOW_USAGE)
                .filter(alert -> !alert.getStatus().equals(AuditAlert.AlertStatus.NEW))
                .count();
            
            long responsesReceived = dayAlerts.stream()
                .filter(alert -> alert.getEmailResponse() != null)
                .count();
            
            double responseRate = emailsSent > 0 ? 
                (double) responsesReceived / emailsSent * 100 : 0.0;
            
            TrendData trend = new TrendData(
                date.toLocalDate(),
                alertsCreated,
                (int) alertsResolved,
                costSaved,
                Math.round(responseRate * 100.0) / 100.0,
                emailsSent,
                responsesReceived
            );
            
            trends.add(trend);
        }
        
        logger.info("Trend data calculated for {} days with {} data points", days, trends.size());
        return trends;
    }
    
    /**
     * Calculate average response time across all alerts
     */
    private double calculateAverageResponseTime() {
        List<AuditAlert> alertsWithResponses = auditAlertRepository.findAll().stream()
            .filter(alert -> alert.getEmailResponse() != null)
            .collect(Collectors.toList());
        
        if (alertsWithResponses.isEmpty()) {
            return 0.0;
        }
        
        double totalHours = 0.0;
        int count = 0;
        
        for (AuditAlert alert : alertsWithResponses) {
            // Find the email send action
            Optional<ActionHistoryLog> emailAction = alert.getActionHistory().stream()
                .filter(action -> action.getActionType() == ActionHistoryLog.ActionType.SEND_EMAIL)
                .findFirst();
            
            if (emailAction.isPresent() && alert.getEmailResponse() != null) {
                long hours = ChronoUnit.HOURS.between(
                    emailAction.get().getCreatedAt(),
                    alert.getEmailResponse().getRespondedAt()
                );
                totalHours += hours;
                count++;
            }
        }
        
        return count > 0 ? totalHours / count : 0.0;
    }
    
    /**
     * Calculate average response time for a specific vendor
     */
    private double calculateVendorAverageResponseTime(String vendorName) {
        List<AuditAlert> vendorAlerts = auditAlertRepository.findAll().stream()
            .filter(alert -> vendorName.equals(alert.getVendorName()))
            .filter(alert -> alert.getEmailResponse() != null)
            .collect(Collectors.toList());
        
        if (vendorAlerts.isEmpty()) {
            return 0.0;
        }
        
        double totalHours = 0.0;
        int count = 0;
        
        for (AuditAlert alert : vendorAlerts) {
            Optional<ActionHistoryLog> emailAction = alert.getActionHistory().stream()
                .filter(action -> action.getActionType() == ActionHistoryLog.ActionType.SEND_EMAIL)
                .findFirst();
            
            if (emailAction.isPresent()) {
                long hours = ChronoUnit.HOURS.between(
                    emailAction.get().getCreatedAt(),
                    alert.getEmailResponse().getRespondedAt()
                );
                totalHours += hours;
                count++;
            }
        }
        
        return count > 0 ? totalHours / count : 0.0;
    }
    
    /**
     * Calculate extension approval rate
     */
    private double calculateExtensionApprovalRate() {
        List<ActionHistoryLog> extensionActions = actionHistoryLogRepository.findAll().stream()
            .filter(action -> action.getActionType() == ActionHistoryLog.ActionType.APPROVE_EXTENSION ||
                             action.getActionType() == ActionHistoryLog.ActionType.REJECT_AND_REVOKE)
            .collect(Collectors.toList());
        
        if (extensionActions.isEmpty()) {
            return 0.0;
        }
        
        long approvals = extensionActions.stream()
            .filter(action -> action.getActionType() == ActionHistoryLog.ActionType.APPROVE_EXTENSION)
            .count();
        
        return (double) approvals / extensionActions.size() * 100;
    }
    
    /**
     * System Statistics DTO
     */
    public static class SystemStatistics {
        private final int totalAlertsProcessed;
        private final double overallResponseRate;
        private final double averageResponseTimeHours;
        private final double extensionApprovalRate;
        private final double revocationRate;
        private final BigDecimal totalCostSavings;
        private final Map<String, Integer> alertsByStatus;
        private final Map<String, Integer> alertsByType;
        
        public SystemStatistics(int totalAlertsProcessed, double overallResponseRate, 
                               double averageResponseTimeHours, double extensionApprovalRate,
                               double revocationRate, BigDecimal totalCostSavings,
                               Map<String, Integer> alertsByStatus, Map<String, Integer> alertsByType) {
            this.totalAlertsProcessed = totalAlertsProcessed;
            this.overallResponseRate = overallResponseRate;
            this.averageResponseTimeHours = averageResponseTimeHours;
            this.extensionApprovalRate = extensionApprovalRate;
            this.revocationRate = revocationRate;
            this.totalCostSavings = totalCostSavings;
            this.alertsByStatus = alertsByStatus;
            this.alertsByType = alertsByType;
        }
        
        // Getters
        public int getTotalAlertsProcessed() { return totalAlertsProcessed; }
        public double getOverallResponseRate() { return overallResponseRate; }
        public double getAverageResponseTimeHours() { return averageResponseTimeHours; }
        public double getExtensionApprovalRate() { return extensionApprovalRate; }
        public double getRevocationRate() { return revocationRate; }
        public BigDecimal getTotalCostSavings() { return totalCostSavings; }
        public Map<String, Integer> getAlertsByStatus() { return alertsByStatus; }
        public Map<String, Integer> getAlertsByType() { return alertsByType; }
    }
    
    /**
     * Vendor Statistics DTO
     */
    public static class VendorStatistics {
        private final String vendorName;
        private final int totalAlerts;
        private final double responseRate;
        private final double averageResponseTime;
        private final BigDecimal costSavings;
        private final int zombieCount;
        private final int lowUsageCount;
        
        public VendorStatistics(String vendorName, int totalAlerts, double responseRate,
                               double averageResponseTime, BigDecimal costSavings,
                               int zombieCount, int lowUsageCount) {
            this.vendorName = vendorName;
            this.totalAlerts = totalAlerts;
            this.responseRate = responseRate;
            this.averageResponseTime = averageResponseTime;
            this.costSavings = costSavings;
            this.zombieCount = zombieCount;
            this.lowUsageCount = lowUsageCount;
        }
        
        // Getters
        public String getVendorName() { return vendorName; }
        public int getTotalAlerts() { return totalAlerts; }
        public double getResponseRate() { return responseRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public BigDecimal getCostSavings() { return costSavings; }
        public int getZombieCount() { return zombieCount; }
        public int getLowUsageCount() { return lowUsageCount; }
    }
    
    /**
     * Trend Data DTO
     */
    public static class TrendData {
        private final java.time.LocalDate date;
        private final int alertsCreated;
        private final int alertsResolved;
        private final BigDecimal costSaved;
        private final double responseRate;
        private final long emailsSent;
        private final long responsesReceived;
        
        public TrendData(java.time.LocalDate date, int alertsCreated, int alertsResolved,
                        BigDecimal costSaved, double responseRate, long emailsSent, long responsesReceived) {
            this.date = date;
            this.alertsCreated = alertsCreated;
            this.alertsResolved = alertsResolved;
            this.costSaved = costSaved;
            this.responseRate = responseRate;
            this.emailsSent = emailsSent;
            this.responsesReceived = responsesReceived;
        }
        
        // Getters
        public java.time.LocalDate getDate() { return date; }
        public int getAlertsCreated() { return alertsCreated; }
        public int getAlertsResolved() { return alertsResolved; }
        public BigDecimal getCostSaved() { return costSaved; }
        public double getResponseRate() { return responseRate; }
        public long getEmailsSent() { return emailsSent; }
        public long getResponsesReceived() { return responsesReceived; }
    }
}