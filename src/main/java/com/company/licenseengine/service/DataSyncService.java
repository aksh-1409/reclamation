package com.company.licenseengine.service;

import com.company.licenseengine.dto.HRISEmployee;
import com.company.licenseengine.dto.VendorLicense;
import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.repository.AuditAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataSyncService {
    
    @Autowired
    private MockApiService mockApiService;
    
    @Autowired
    private AuditAlertRepository auditAlertRepository;
    
    /**
     * Main synchronization method - Phase 1 of the workflow
     */
    public SyncResult synchronizeData() {
        // Step 1: Get data from external sources
        List<HRISEmployee> employees = mockApiService.getEmployeesFromHRIS();
        List<VendorLicense> licenses = mockApiService.getLicensesFromVendor();
        
        // Step 2: Create lookup maps for efficient processing
        Map<String, HRISEmployee> employeeMap = employees.stream()
            .collect(Collectors.toMap(HRISEmployee::getEmail, emp -> emp));
        
        // Step 3: Process each license and apply business rules
        int zombieCount = 0;
        int lowUsageCount = 0;
        
        for (VendorLicense license : licenses) {
            String email = license.getEmail();
            HRISEmployee employee = employeeMap.get(email);
            
            // Skip if license is already being processed
            Optional<AuditAlert> existingAlert = auditAlertRepository
                .findByEmailAndVendorNameAndStatusNot(email, license.getVendorName(), AuditAlert.AlertStatus.RESOLVED);
            
            if (existingAlert.isPresent()) {
                continue; // Skip already processed licenses
            }
            
            AuditAlert alert = null;
            
            // Rule A: ZOMBIE - License exists but employee is terminated
            if (employee == null || employee.isTerminated()) {
                alert = new AuditAlert(email, 
                    employee != null ? employee.getName() : "Unknown Employee", 
                    license.getVendorName(), 
                    AuditAlert.AlertType.ZOMBIE);
                alert.setLastLoginDate(license.getLastLoginDate());
                alert.setMonthlyCost(license.getMonthlyCost()); // Set cost data
                zombieCount++;
            }
            // Rule B: LOW_USAGE - Employee is active but hasn't logged in for 90+ days
            else if (employee.isActive() && license.isLowUsage()) {
                alert = new AuditAlert(email, 
                    employee.getName(), 
                    license.getVendorName(), 
                    AuditAlert.AlertType.LOW_USAGE);
                alert.setLastLoginDate(license.getLastLoginDate());
                alert.setMonthlyCost(license.getMonthlyCost()); // Set cost data
                lowUsageCount++;
            }
            
            // Save the alert if one was created
            if (alert != null) {
                auditAlertRepository.save(alert);
            }
        }
        
        return new SyncResult(zombieCount, lowUsageCount, employees.size(), licenses.size());
    }
    
    /**
     * Result object for sync operation
     */
    public static class SyncResult {
        private final int zombieAlertsCreated;
        private final int lowUsageAlertsCreated;
        private final int totalEmployees;
        private final int totalLicenses;
        
        public SyncResult(int zombieAlertsCreated, int lowUsageAlertsCreated, int totalEmployees, int totalLicenses) {
            this.zombieAlertsCreated = zombieAlertsCreated;
            this.lowUsageAlertsCreated = lowUsageAlertsCreated;
            this.totalEmployees = totalEmployees;
            this.totalLicenses = totalLicenses;
        }
        
        public int getZombieAlertsCreated() { return zombieAlertsCreated; }
        public int getLowUsageAlertsCreated() { return lowUsageAlertsCreated; }
        public int getTotalEmployees() { return totalEmployees; }
        public int getTotalLicenses() { return totalLicenses; }
        public int getTotalAlertsCreated() { return zombieAlertsCreated + lowUsageAlertsCreated; }
    }
}