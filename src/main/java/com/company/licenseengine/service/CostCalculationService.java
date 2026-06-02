package com.company.licenseengine.service;

import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.repository.AuditAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CostCalculationService.class);
    
    @Autowired
    private AuditAlertRepository auditAlertRepository;
    
    /**
     * Calculate total cost savings from resolved alerts
     */
    public BigDecimal calculateTotalSavings() {
        List<AuditAlert> resolvedAlerts = auditAlertRepository.findResolvedAlerts();
        
        logger.info("DEBUG: Found {} resolved alerts", resolvedAlerts.size());
        
        BigDecimal totalSavings = BigDecimal.ZERO;
        for (AuditAlert alert : resolvedAlerts) {
            logger.info("DEBUG: Alert ID {}, Cost Saved: {}, Monthly Cost: {}", 
                alert.getId(), alert.getCostSaved(), alert.getMonthlyCost());
            if (alert.getCostSaved() != null) {
                totalSavings = totalSavings.add(alert.getCostSaved());
            }
        }
        
        logger.info("DEBUG: Calculated total savings: ${}", totalSavings);
        return totalSavings;
    }
    
    /**
     * Calculate monthly savings projection
     */
    public BigDecimal calculateMonthlySavings() {
        List<AuditAlert> resolvedAlerts = auditAlertRepository.findResolvedAlerts();
        
        BigDecimal monthlySavings = BigDecimal.ZERO;
        for (AuditAlert alert : resolvedAlerts) {
            if (alert.getMonthlyCost() != null) {
                monthlySavings = monthlySavings.add(alert.getMonthlyCost());
            }
        }
        
        return monthlySavings;
    }
    
    /**
     * Calculate annual savings projection
     */
    public BigDecimal calculateAnnualSavings() {
        BigDecimal monthlySavings = calculateMonthlySavings();
        return monthlySavings.multiply(BigDecimal.valueOf(12));
    }
    
    /**
     * Calculate cost savings by vendor
     */
    public Map<String, BigDecimal> calculateSavingsByVendor() {
        List<AuditAlert> resolvedAlerts = auditAlertRepository.findResolvedAlerts();
        Map<String, BigDecimal> savingsByVendor = new HashMap<>();
        
        for (AuditAlert alert : resolvedAlerts) {
            if (alert.getCostSaved() != null && alert.getVendorName() != null) {
                savingsByVendor.merge(alert.getVendorName(), alert.getCostSaved(), BigDecimal::add);
            }
        }
        
        return savingsByVendor;
    }
    
    /**
     * Calculate cost saved for a specific alert when it's resolved
     */
    public BigDecimal calculateCostSavedForAlert(AuditAlert alert) {
        if (alert.getMonthlyCost() == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate how many months of cost we're saving
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = alert.getCreatedAt();
        
        // For simplicity, assume we save the full monthly cost
        // In a real scenario, you might calculate partial months
        BigDecimal costSaved = alert.getMonthlyCost();
        
        // If the alert was created more than a month ago, calculate additional savings
        long monthsBetween = ChronoUnit.MONTHS.between(createdAt, now);
        if (monthsBetween > 0) {
            costSaved = costSaved.multiply(BigDecimal.valueOf(monthsBetween + 1));
        }
        
        return costSaved.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get cost savings summary
     */
    public CostSavingsSummary getCostSavingsSummary() {
        BigDecimal totalSavings = calculateTotalSavings();
        BigDecimal monthlySavings = calculateMonthlySavings();
        BigDecimal annualSavings = calculateAnnualSavings();
        Map<String, BigDecimal> savingsByVendor = calculateSavingsByVendor();
        
        return new CostSavingsSummary(totalSavings, monthlySavings, annualSavings, savingsByVendor);
    }
    
    /**
     * Cost savings summary DTO
     */
    public static class CostSavingsSummary {
        private final BigDecimal totalSavings;
        private final BigDecimal monthlySavings;
        private final BigDecimal annualSavings;
        private final Map<String, BigDecimal> savingsByVendor;
        
        public CostSavingsSummary(BigDecimal totalSavings, BigDecimal monthlySavings, 
                                 BigDecimal annualSavings, Map<String, BigDecimal> savingsByVendor) {
            this.totalSavings = totalSavings;
            this.monthlySavings = monthlySavings;
            this.annualSavings = annualSavings;
            this.savingsByVendor = savingsByVendor;
        }
        
        public BigDecimal getTotalSavings() { return totalSavings; }
        public BigDecimal getMonthlySavings() { return monthlySavings; }
        public BigDecimal getAnnualSavings() { return annualSavings; }
        public Map<String, BigDecimal> getSavingsByVendor() { return savingsByVendor; }
    }
}