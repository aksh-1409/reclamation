package com.company.licenseengine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "action_history_log")
public class ActionHistoryLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "audit_alert_id", nullable = false)
    private AuditAlert auditAlert;
    
    @Column(name = "admin_username", nullable = false)
    private String adminUsername;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;
    
    @Column(columnDefinition = "TEXT")
    private String justification;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "previous_status")
    @Enumerated(EnumType.STRING)
    private AuditAlert.AlertStatus previousStatus;
    
    @Column(name = "new_status")
    @Enumerated(EnumType.STRING)
    private AuditAlert.AlertStatus newStatus;
    
    // Constructors
    public ActionHistoryLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ActionHistoryLog(AuditAlert auditAlert, String adminUsername, ActionType actionType, 
                           String justification, AuditAlert.AlertStatus previousStatus, 
                           AuditAlert.AlertStatus newStatus) {
        this();
        this.auditAlert = auditAlert;
        this.adminUsername = adminUsername;
        this.actionType = actionType;
        this.justification = justification;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public AuditAlert getAuditAlert() { return auditAlert; }
    public void setAuditAlert(AuditAlert auditAlert) { this.auditAlert = auditAlert; }
    
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public AuditAlert.AlertStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(AuditAlert.AlertStatus previousStatus) { this.previousStatus = previousStatus; }
    
    public AuditAlert.AlertStatus getNewStatus() { return newStatus; }
    public void setNewStatus(AuditAlert.AlertStatus newStatus) { this.newStatus = newStatus; }
    
    // Enum
    public enum ActionType {
        REVOKE_ZOMBIE, SEND_EMAIL, APPROVE_EXTENSION, REJECT_AND_REVOKE, 
        EXTEND_DEADLINE, REVOKE_OVERDUE, REVOKE_EXPIRED, SEND_REMINDER
    }
}