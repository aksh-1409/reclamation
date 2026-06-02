package com.company.licenseengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class VendorLicense {
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("lastLoginDate")
    private LocalDateTime lastLoginDate;
    
    @JsonProperty("licenseType")
    private String licenseType;
    
    @JsonProperty("vendorName")
    private String vendorName;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    // Constructors
    public VendorLicense() {}
    
    public VendorLicense(String email, String userId, LocalDateTime lastLoginDate, 
                        String licenseType, String vendorName, boolean isActive) {
        this.email = email;
        this.userId = userId;
        this.lastLoginDate = lastLoginDate;
        this.licenseType = licenseType;
        this.vendorName = vendorName;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isLowUsage() {
        if (lastLoginDate == null) return true;
        return lastLoginDate.isBefore(LocalDateTime.now().minusDays(90));
    }
}