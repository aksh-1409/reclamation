package com.company.licenseengine.repository;

import com.company.licenseengine.entity.AuditAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditAlertRepository extends JpaRepository<AuditAlert, Long> {
    
    // Find alerts visible on dashboard (excluding hidden states)
    @Query("SELECT a FROM AuditAlert a WHERE a.status IN ('NEW', 'READY_FOR_REVIEW', 'RESPONSE_OVERDUE', 'EXTENSION_EXPIRED') ORDER BY a.createdAt DESC")
    List<AuditAlert> findVisibleAlerts();
    
    // Find alerts by status
    List<AuditAlert> findByStatusOrderByCreatedAtDesc(AuditAlert.AlertStatus status);
    
    // Find alerts by type and status
    List<AuditAlert> findByAlertTypeAndStatusOrderByCreatedAtDesc(AuditAlert.AlertType alertType, AuditAlert.AlertStatus status);
    
    // Find overdue responses
    @Query("SELECT a FROM AuditAlert a WHERE a.status = 'AWAITING_RESPONSE' AND a.responseDeadline < :currentTime")
    List<AuditAlert> findOverdueResponses(@Param("currentTime") LocalDateTime currentTime);
    
    // Find expired extensions
    @Query("SELECT a FROM AuditAlert a WHERE a.status = 'APPROVED_EXTENSION' AND a.extensionExpiration < :currentTime")
    List<AuditAlert> findExpiredExtensions(@Param("currentTime") LocalDateTime currentTime);
    
    // Find by verification token
    Optional<AuditAlert> findByVerificationToken(String verificationToken);
    
    // Find by email and vendor
    Optional<AuditAlert> findByEmailAndVendorNameAndStatusNot(String email, String vendorName, AuditAlert.AlertStatus status);
    
    // Count alerts by status
    long countByStatus(AuditAlert.AlertStatus status);
    
    // Count alerts by type
    long countByAlertType(AuditAlert.AlertType alertType);
    
    // Find resolved alerts for history
    @Query("SELECT a FROM AuditAlert a WHERE a.status = 'RESOLVED' ORDER BY a.createdAt DESC")
    List<AuditAlert> findResolvedAlerts();
}