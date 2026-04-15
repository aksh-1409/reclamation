package com.company.licenseengine.controller;

import com.company.licenseengine.entity.AuditAlert;
import com.company.licenseengine.service.AuditAlertService;
import com.company.licenseengine.service.DataSyncService;
import com.company.licenseengine.service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class DashboardController {
    
    @Autowired
    private AuditAlertService auditAlertService;
    
    @Autowired
    private DataSyncService dataSyncService;
    
    @Autowired
    private ScheduledTaskService scheduledTaskService;
    
    /**
     * Main dashboard page
     */
    @GetMapping
    public String dashboard(Model model) {
        List<AuditAlert> alerts = auditAlertService.getVisibleAlerts();
        model.addAttribute("alerts", alerts);
        model.addAttribute("zombieCount", alerts.stream().filter(a -> a.getAlertType() == AuditAlert.AlertType.ZOMBIE).count());
        model.addAttribute("lowUsageCount", alerts.stream().filter(a -> a.getAlertType() == AuditAlert.AlertType.LOW_USAGE).count());
        return "dashboard";
    }
    
    /**
     * Sync data from external APIs
     */
    @PostMapping("/sync")
    public String syncData(RedirectAttributes redirectAttributes) {
        try {
            DataSyncService.SyncResult result = dataSyncService.synchronizeData();
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Sync completed! Created %d zombie alerts and %d low usage alerts from %d employees and %d licenses.", 
                    result.getZombieAlertsCreated(), result.getLowUsageAlertsCreated(), 
                    result.getTotalEmployees(), result.getTotalLicenses()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sync failed: " + e.getMessage());
        }
        return "redirect:/";
    }
    
    /**
     * Revoke zombie license
     */
    @PostMapping("/revoke-zombie/{id}")
    public String revokeZombie(@PathVariable Long id, 
                              @RequestParam String justification,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        
        if (justification == null || justification.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Justification is required for revoking licenses.");
            return "redirect:/";
        }
        
        boolean success = auditAlertService.revokeZombieLicense(id, auth.getName(), justification);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Zombie license revoked successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to revoke license.");
        }
        
        return "redirect:/";
    }
    
    /**
     * Send verification email for low usage
     */
    @PostMapping("/send-email/{id}")
    public String sendEmail(@PathVariable Long id, 
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        
        boolean success = auditAlertService.sendVerificationEmail(id, auth.getName());
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Verification email sent successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send email.");
        }
        
        return "redirect:/";
    }
    
    /**
     * Approve extension request
     */
    @PostMapping("/approve-extension/{id}")
    public String approveExtension(@PathVariable Long id,
                                  @RequestParam String justification,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        
        if (justification == null || justification.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Justification is required for approving extensions.");
            return "redirect:/";
        }
        
        boolean success = auditAlertService.approveExtension(id, auth.getName(), justification);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Extension approved successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve extension.");
        }
        
        return "redirect:/";
    }
    
    /**
     * Reject extension and revoke license
     */
    @PostMapping("/reject-and-revoke/{id}")
    public String rejectAndRevoke(@PathVariable Long id,
                                 @RequestParam String justification,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        
        if (justification == null || justification.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Justification is required for rejecting extensions.");
            return "redirect:/";
        }
        
        boolean success = auditAlertService.rejectAndRevoke(id, auth.getName(), justification);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Extension rejected and license revoked.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject and revoke.");
        }
        
        return "redirect:/";
    }
    
    /**
     * Extend deadline for overdue response
     */
    @PostMapping("/extend-deadline/{id}")
    public String extendDeadline(@PathVariable Long id,
                                @RequestParam int additionalDays,
                                @RequestParam String justification,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        
        if (justification == null || justification.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Justification is required for extending deadlines.");
            return "redirect:/";
        }
        
        if (additionalDays <= 0 || additionalDays > 30) {
            redirectAttributes.addFlashAttribute("errorMessage", "Additional days must be between 1 and 30.");
            return "redirect:/";
        }
        
        boolean success = auditAlertService.extendDeadline(id, auth.getName(), additionalDays, justification);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Deadline extended successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to extend deadline.");
        }
        
        return "redirect:/";
    }
    
    /**
     * Revoke overdue license
     */
    @PostMapping("/revoke-overdue/{id}")
    public String revokeOverdue(@PathVariable Long id,
                               @RequestParam String justification,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        
        if (justification == null || justification.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Justification is required for revoking overdue licenses.");
            return "redirect:/";
        }
        
        boolean success = auditAlertService.revokeOverdueLicense(id, auth.getName(), justification);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Overdue license revoked successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to revoke overdue license.");
        }
        
        return "redirect:/";
    }
    
    /**
     * View alert details (AJAX endpoint)
     */
    @GetMapping("/alert/{id}")
    @ResponseBody
    public Optional<AuditAlert> getAlertDetails(@PathVariable Long id) {
        return auditAlertService.getAlertById(id);
    }
    
    /**
     * History page
     */
    @GetMapping("/history")
    public String history(Model model) {
        List<AuditAlert> resolvedAlerts = auditAlertService.getResolvedAlerts();
        model.addAttribute("resolvedAlerts", resolvedAlerts);
        return "history";
    }
    
    /**
     * Manual trigger for scheduled tasks (for testing)
     */
    @PostMapping("/manual-checks")
    public String manualChecks(RedirectAttributes redirectAttributes) {
        int overdueCount = scheduledTaskService.manualDeadlineCheck();
        int expiredCount = scheduledTaskService.manualExtensionCheck();
        
        redirectAttributes.addFlashAttribute("successMessage", 
            String.format("Manual checks completed. Found %d overdue responses and %d expired extensions.", 
                overdueCount, expiredCount));
        
        return "redirect:/";
    }
}