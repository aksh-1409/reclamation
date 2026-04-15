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
            // Active employees (12 total)
            new HRISEmployee("john.doe@company.com", "John Doe", "Active", "Engineering"),
            new HRISEmployee("jane.smith@company.com", "Jane Smith", "Active", "Marketing"),
            new HRISEmployee("alice.brown@company.com", "Alice Brown", "Active", "HR"),
            new HRISEmployee("diana.miller@company.com", "Diana Miller", "Active", "Engineering"),
            new HRISEmployee("frank.garcia@company.com", "Frank Garcia", "Active", "Operations"),
            new HRISEmployee("coderaksh0777@gmail.com", "Aksh", "Active", "Engineering"), // Real email for testing
            new HRISEmployee("sarah.johnson@company.com", "Sarah Johnson", "Active", "Design"),
            new HRISEmployee("mike.chen@company.com", "Mike Chen", "Active", "DevOps"),
            new HRISEmployee("lisa.wang@company.com", "Lisa Wang", "Active", "Product"),
            new HRISEmployee("david.kumar@company.com", "David Kumar", "Active", "QA"),
            new HRISEmployee("emma.taylor@company.com", "Emma Taylor", "Active", "Marketing"),
            new HRISEmployee("ryan.patel@company.com", "Ryan Patel", "Active", "Sales"),
            
            // Terminated employees (7 total)
            new HRISEmployee("bob.wilson@company.com", "Bob Wilson", "Terminated", "Sales"),
            new HRISEmployee("charlie.davis@company.com", "Charlie Davis", "Terminated", "Finance"),
            new HRISEmployee("grace.lee@company.com", "Grace Lee", "Terminated", "Marketing"),
            new HRISEmployee("tom.anderson@company.com", "Tom Anderson", "Terminated", "Engineering"),
            new HRISEmployee("maria.gonzalez@company.com", "Maria Gonzalez", "Terminated", "HR"),
            new HRISEmployee("kevin.wright@company.com", "Kevin Wright", "Terminated", "Operations"),
            new HRISEmployee("nancy.clark@company.com", "Nancy Clark", "Terminated", "Finance")
        );
    }
    
    /**
     * Mock Vendor API - Returns list of active licenses with last login dates
     */
    public List<VendorLicense> getLicensesFromVendor() {
        LocalDateTime now = LocalDateTime.now();
        
        return Arrays.asList(
            // Active users with recent logins (8 licenses)
            new VendorLicense("john.doe@company.com", "user001", now.minusDays(5), "Premium", "Zoom", true),
            new VendorLicense("jane.smith@company.com", "user002", now.minusDays(10), "Basic", "Jira", true),
            new VendorLicense("diana.miller@company.com", "user006", now.minusDays(2), "Premium", "Slack", true),
            new VendorLicense("sarah.johnson@company.com", "user011", now.minusDays(7), "Enterprise", "Figma", true),
            new VendorLicense("mike.chen@company.com", "user012", now.minusDays(3), "Premium", "Docker Hub", true),
            new VendorLicense("lisa.wang@company.com", "user013", now.minusDays(12), "Basic", "Notion", true),
            new VendorLicense("emma.taylor@company.com", "user015", now.minusDays(8), "Premium", "Canva", true),
            new VendorLicense("ryan.patel@company.com", "user016", now.minusDays(4), "Enterprise", "HubSpot", true),
            
            // Active users with old logins - LOW_USAGE candidates (5 licenses)
            new VendorLicense("alice.brown@company.com", "user004", now.minusDays(120), "Premium", "Adobe Creative", true),
            new VendorLicense("frank.garcia@company.com", "user007", now.minusDays(95), "Enterprise", "Salesforce", true),
            new VendorLicense("coderaksh0777@gmail.com", "user017", now.minusDays(110), "Premium", "IntelliJ IDEA", true), // Real email for testing
            new VendorLicense("david.kumar@company.com", "user014", now.minusDays(105), "Basic", "Postman", true),
            new VendorLicense("test.lowusage@company.com", "user010", now.minusDays(100), "Enterprise", "Confluence", true),
            
            // Terminated employees still with licenses - ZOMBIE candidates (7 licenses)
            new VendorLicense("bob.wilson@company.com", "user003", now.minusDays(30), "Basic", "Office 365", true),
            new VendorLicense("charlie.davis@company.com", "user005", now.minusDays(60), "Premium", "Tableau", true),
            new VendorLicense("grace.lee@company.com", "user008", now.minusDays(45), "Basic", "Zoom", true),
            new VendorLicense("tom.anderson@company.com", "user018", now.minusDays(25), "Premium", "GitHub Pro", true),
            new VendorLicense("maria.gonzalez@company.com", "user019", now.minusDays(40), "Enterprise", "BambooHR", true),
            new VendorLicense("kevin.wright@company.com", "user020", now.minusDays(35), "Basic", "Trello", true),
            new VendorLicense("nancy.clark@company.com", "user021", now.minusDays(50), "Premium", "QuickBooks", true),
            
            // Additional edge cases (5 licenses)
            new VendorLicense("test.zombie@company.com", "user009", now.minusDays(15), "Premium", "Figma", true),
            new VendorLicense("unknown.user@company.com", "user022", now.minusDays(20), "Basic", "Dropbox", true),
            new VendorLicense("john.doe@company.com", "user023", now.minusDays(6), "Basic", "Spotify", true), // Multiple licenses for same user
            new VendorLicense("jane.smith@company.com", "user024", now.minusDays(130), "Premium", "Adobe Photoshop", true), // Same user, different usage pattern
            new VendorLicense("expired.license@company.com", "user025", now.minusDays(200), "Enterprise", "Legacy Tool", true)
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