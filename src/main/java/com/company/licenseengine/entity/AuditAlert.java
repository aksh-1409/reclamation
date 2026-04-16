package com.company.licenseengine.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_alerts")
public class AuditAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "employee_name")
    private String employeeName;
    
    @Column(name = "vendor_name")
    private String vendorName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;
    
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;
    
    @Column(name = "extension_expiration")
    private LocalDateTime extensionExpiration;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    @Column(name = "monthly_cost", precision = 10, scale = 2)
    private BigDecimal monthlyCost; // Monthly license cost
    
    @Column(name = "cost_saved", precision = 10, scale = 2)
    private BigDecimal costSaved; // Cost saved when license is revoked
    
    @OneToMany(mappedBy = "auditAlert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActionHistoryLog> actionHistory = new ArrayList<>();
    
    @OneToOne(mappedBy = "auditAlert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EmailResponse emailResponse;
    
    // Constructors
    public AuditAlert() {
        this.createdAt = LocalDateTime.now();
        this.status = AlertStatus.NEW;
    }
    
    public AuditAlert(String email, String employeeName, String vendorName, AlertType alertType) {
        this();
        this.email = email;
        this.employeeName = employeeName;
        this.vendorName = vendorName;
        this.alertType = alertType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    
    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDateTime responseDeadline) { this.responseDeadline = responseDeadline; }
    
    public LocalDateTime getExtensionExpiration() { return extensionExpiration; }
    public void setExtensionExpiration(LocalDateTime extensionExpiration) { this.extensionExpiration = extensionExpiration; }
    
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    
    public Boolean getReminderSent() { return reminderSent; }
    public void setReminderSent(Boolean reminderSent) { this.reminderSent = reminderSent; }
    
    public BigDecimal getMonthlyCost() { return monthlyCost; }
    public void setMonthlyCost(BigDecimal monthlyCost) { this.monthlyCost = monthlyCost; }
    
    public BigDecimal getCostSaved() { return costSaved; }
    public void setCostSaved(BigDecimal costSaved) { this.costSaved = costSaved; }
    
    public List<ActionHistoryLog> getActionHistory() { return actionHistory; }
    public void setActionHistory(List<ActionHistoryLog> actionHistory) { this.actionHistory = actionHistory; }
    
    public EmailResponse getEmailResponse() { return emailResponse; }
    public void setEmailResponse(EmailResponse emailResponse) { this.emailResponse = emailResponse; }
    
    // Enums
    public enum AlertType {
        ZOMBIE, LOW_USAGE
    }
    
    public enum AlertStatus {
        NEW, AWAITING_RESPONSE, READY_FOR_REVIEW, RESPONSE_OVERDUE, 
        APPROVED_EXTENSION, EXTENSION_EXPIRED, RESOLVED
    }
}