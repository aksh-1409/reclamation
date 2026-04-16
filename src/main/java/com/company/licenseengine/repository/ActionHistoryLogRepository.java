package com.company.licenseengine.repository;

import com.company.licenseengine.entity.ActionHistoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionHistoryLogRepository extends JpaRepository<ActionHistoryLog, Long> {
    
    // Find all action history ordered by creation date (newest first) with audit alert data
    @Query("SELECT ah FROM ActionHistoryLog ah JOIN FETCH ah.auditAlert ORDER BY ah.createdAt DESC")
    List<ActionHistoryLog> findAllWithAuditAlertByOrderByCreatedAtDesc();
    
    // Find all action history ordered by creation date (newest first)
    List<ActionHistoryLog> findAllByOrderByCreatedAtDesc();
    
    // Find action history for a specific alert
    List<ActionHistoryLog> findByAuditAlertIdOrderByCreatedAtDesc(Long auditAlertId);
    
    // Find actions by admin
    List<ActionHistoryLog> findByAdminUsernameOrderByCreatedAtDesc(String adminUsername);
    
    // Find actions by type
    List<ActionHistoryLog> findByActionTypeOrderByCreatedAtDesc(ActionHistoryLog.ActionType actionType);
}