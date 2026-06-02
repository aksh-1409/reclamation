package com.company.licenseengine.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    /**
     * Send verification email to user
     */
    public boolean sendVerificationEmail(String toEmail, String employeeName, String vendorName, 
                                       String verificationUrl, String responseDeadline) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Email details
            helper.setTo(toEmail);
            helper.setFrom("akshusrathe007@gmail.com");
            helper.setSubject("Action Required: Verify Your License Usage - " + vendorName);
            
            // Create template context
            Context context = new Context();
            context.setVariable("employeeName", employeeName);
            context.setVariable("vendorName", vendorName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("responseDeadline", responseDeadline);
            
            // Generate HTML content from Thymeleaf template
            String htmlContent = templateEngine.process("email/verification-email", context);
            helper.setText(htmlContent, true);
            
            // Send email
            mailSender.send(message);
            System.out.println("✅ Verification email sent successfully to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send verification email to: " + toEmail);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Send reminder email to user
     */
    public boolean sendReminderEmail(String toEmail, String employeeName, String vendorName, 
                                   String verificationUrl, String responseDeadline) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Email details
            helper.setTo(toEmail);
            helper.setFrom("akshusrathe007@gmail.com");
            helper.setSubject("REMINDER: License Verification Required - " + vendorName + " (Expires Soon)");
            
            // Create template context
            Context context = new Context();
            context.setVariable("employeeName", employeeName);
            context.setVariable("vendorName", vendorName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("responseDeadline", responseDeadline);
            
            // Generate HTML content from Thymeleaf template
            String htmlContent = templateEngine.process("email/reminder-email", context);
            helper.setText(htmlContent, true);
            
            // Send email
            mailSender.send(message);
            System.out.println("✅ Reminder email sent successfully to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send reminder email to: " + toEmail);
            e.printStackTrace();
            return false;
        }
    }
}