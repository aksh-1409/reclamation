package com.company.licenseengine.controller;

import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.entity.EmailResponse;
import com.company.licenseengine.service.AuditAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/verify")
public class VerificationController {
    
    @Autowired
    private AuditAlertService auditAlertService;
    
    /**
     * Handle root /verify access (without token)
     */
    @GetMapping
    public String verifyRoot(Model model) {
        model.addAttribute("error", "Invalid verification link. Please use the link provided in your email.");
        return "verification-error";
    }
    
    /**
     * Display verification form for user
     */
    @GetMapping("/{token}")
    public String showVerificationForm(@PathVariable String token, Model model) {
        Optional<AuditAlert> alertOpt = auditAlertService.getAlertByToken(token);
        
        if (alertOpt.isEmpty()) {
            model.addAttribute("error", "Invalid or expired verification link.");
            return "verification-error";
        }
        
        AuditAlert alert = alertOpt.get();
        
        // Allow verification for AWAITING_RESPONSE and RESPONSE_OVERDUE statuses
        if (alert.getStatus() != AuditAlert.AlertStatus.AWAITING_RESPONSE && 
            alert.getStatus() != AuditAlert.AlertStatus.RESPONSE_OVERDUE) {
            model.addAttribute("error", "This verification link has already been used or has expired.");
            return "verification-error";
        }
        
        model.addAttribute("alert", alert);
        model.addAttribute("token", token);
        return "verification-form";
    }
    
    /**
     * Process user's verification response
     */
    @PostMapping("/{token}")
    public String processVerification(@PathVariable String token,
                                    @RequestParam String responseType,
                                    @RequestParam(required = false) String reason,
                                    @RequestParam(required = false) Integer durationDays,
                                    RedirectAttributes redirectAttributes) {
        
        Optional<AuditAlert> alertOpt = auditAlertService.getAlertByToken(token);
        
        if (alertOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired verification link.");
            return "redirect:/verify/error";
        }
        
        AuditAlert alert = alertOpt.get();
        
        EmailResponse.ResponseType type;
        try {
            type = EmailResponse.ResponseType.valueOf(responseType);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid response type.");
            return "redirect:/verify/error";
        }
        
        // Validate input based on response type
        if (type == EmailResponse.ResponseType.KEEP_LICENSE) {
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Reason is required when keeping the license.");
                return "redirect:/verify/" + token;
            }
            if (durationDays == null || durationDays <= 0 || durationDays > 365) {
                redirectAttributes.addFlashAttribute("error", "Duration must be between 1 and 365 days.");
                return "redirect:/verify/" + token;
            }
        }
        
        boolean success = auditAlertService.processUserResponse(token, type, reason, durationDays);
        
        if (success) {
            if (type == EmailResponse.ResponseType.SURRENDER_LICENSE) {
                redirectAttributes.addFlashAttribute("message", 
                    "Thank you for your response. Your license has been revoked as requested.");
            } else {
                redirectAttributes.addFlashAttribute("message", 
                    "Thank you for your response. Your request has been forwarded to the admin for review.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to process your response. Please try again.");
        }
        
        return "redirect:/verify/success";
    }
    
    /**
     * Success page after verification
     */
    @GetMapping("/success")
    public String verificationSuccess() {
        return "verification-success";
    }
    
    /**
     * Error page for verification issues
     */
    @GetMapping("/error")
    public String verificationError() {
        return "verification-error";
    }
}