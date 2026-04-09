package com.company.licenseengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HRISEmployee {
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("status")
    private String status; // "Active" or "Terminated"
    
    @JsonProperty("department")
    private String department;
    
    // Constructors
    public HRISEmployee() {}
    
    public HRISEmployee(String email, String name, String status, String department) {
        this.email = email;
        this.name = name;
        this.status = status;
        this.department = department;
    }
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }
    
    public boolean isTerminated() {
        return "Terminated".equalsIgnoreCase(status);
    }
}