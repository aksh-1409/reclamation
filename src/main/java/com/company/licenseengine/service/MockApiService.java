package com.company.licenseengine.service;

import com.company.licenseengine.dto.HRISEmployee;
import com.company.licenseengine.dto.VendorLicense;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MockApiService {
    
    /**
     * Mock HRIS API - Returns list of employees with their status
     */
    public List<HRISEmployee> getEmployeesFromHRIS() {
        return Arrays.asList(
            new HRISEmployee("john.doe@company.com", "John Doe", "Active", "Engineering"),
            new HRISEmployee("jane.smith@company.com", "Jane Smith", "Active", "Marketing"),
            new HRISEmployee("bob.wilson@company.com", "Bob Wilson", "Terminated", "Sales"),
            new HRISEmployee("alice.brown@company.com", "Alice Brown", "Active", "HR"),
            new HRISEmployee("charlie.davis@company.com", "Charlie Davis", "Terminated", "Finance"),
            new HRISEmployee("diana.miller@company.com", "Diana Miller", "Active", "Engineering"),
            new HRISEmployee("frank.garcia@company.com", "Frank Garcia", "Active", "Operations"),
            new HRISEmployee("grace.lee@company.com", "Grace Lee", "Terminated", "Marketing")
        );
    }
    
    /**
     * Mock Vendor API - Returns list of active licenses with last login dates
     */
    public List<VendorLicense> getLicensesFromVendor() {
        LocalDateTime now = LocalDateTime.now();
        
        return Arrays.asList(
            // Active users with recent logins
            new VendorLicense("john.doe@company.com", "user001", now.minusDays(5), "Premium", "Zoom", true),
            new VendorLicense("jane.smith@company.com", "user002", now.minusDays(10), "Basic", "Jira", true),
            new VendorLicense("diana.miller@company.com", "user006", now.minusDays(2), "Premium", "Slack", true),
            
            // Active users with old logins (LOW_USAGE candidates)
            new VendorLicense("alice.brown@company.com", "user004", now.minusDays(120), "Premium", "Adobe Creative", true),
            new VendorLicense("frank.garcia@company.com", "user007", now.minusDays(95), "Enterprise", "Salesforce", true),
            
            // Terminated employees still with licenses (ZOMBIE candidates)
            new VendorLicense("bob.wilson@company.com", "user003", now.minusDays(30), "Basic", "Office 365", true),
            new VendorLicense("charlie.davis@company.com", "user005", now.minusDays(60), "Premium", "Tableau", true),
            new VendorLicense("grace.lee@company.com", "user008", now.minusDays(45), "Basic", "Zoom", true),
            
            // Additional test cases
            new VendorLicense("test.zombie@company.com", "user009", now.minusDays(15), "Premium", "Figma", true),
            new VendorLicense("test.lowusage@company.com", "user010", now.minusDays(100), "Enterprise", "Confluence", true)
        );
    }
    
    /**
     * Mock API call to revoke a license
     */
    public boolean revokeLicense(String email, String vendorName) {
        // Simulate API call delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock success response (in real implementation, this would call actual vendor API)
        System.out.println("MOCK API: Revoking license for " + email + " from " + vendorName);
        return true;
    }
    
    /**
     * Mock email sending service
     */
    public boolean sendVerificationEmail(String email, String verificationUrl) {
        // Simulate email sending
        System.out.println("MOCK EMAIL: Sending verification email to " + email);
        System.out.println("Verification URL: " + verificationUrl);
        return true;
    }
    
    /**
     * Mock reminder email sending
     */
    public boolean sendReminderEmail(String email, String verificationUrl) {
        // Simulate reminder email sending
        System.out.println("MOCK EMAIL: Sending reminder email to " + email);
        System.out.println("Verification URL: " + verificationUrl);
        return true;
    }
}