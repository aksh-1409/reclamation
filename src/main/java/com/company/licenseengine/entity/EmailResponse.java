package com.company.licenseengine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_responses")
public class EmailResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_alert_id", nullable = false)
    private AuditAlert auditAlert;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponseType responseType;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "requested_duration_days")
    private Integer requestedDurationDays;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    // Constructors
    public EmailResponse() {
        this.respondedAt = LocalDateTime.now();
    }
    
    public EmailResponse(AuditAlert auditAlert, ResponseType responseType, String reason, Integer requestedDurationDays) {
        this();
        this.auditAlert = auditAlert;
        this.responseType = responseType;
        this.reason = reason;
        this.requestedDurationDays = requestedDurationDays;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public AuditAlert getAuditAlert() { return auditAlert; }
    public void setAuditAlert(AuditAlert auditAlert) { this.auditAlert = auditAlert; }
    
    public ResponseType getResponseType() { return responseType; }
    public void setResponseType(ResponseType responseType) { this.responseType = responseType; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Integer getRequestedDurationDays() { return requestedDurationDays; }
    public void setRequestedDurationDays(Integer requestedDurationDays) { this.requestedDurationDays = requestedDurationDays; }
    
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    
    // Enum
    public enum ResponseType {
        SURRENDER_LICENSE, KEEP_LICENSE, KEEP_LICENSE_PERMANENT, KEEP_LICENSE_TEMPORARY
    }
}