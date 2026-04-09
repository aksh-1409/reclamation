package com.company.licenseengine.repository;

import com.company.licenseengine.entity.EmailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailResponseRepository extends JpaRepository<EmailResponse, Long> {
    
    // Find response by audit alert ID
    Optional<EmailResponse> findByAuditAlertId(Long auditAlertId);
}