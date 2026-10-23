package com.ruralhealthcare.medicine.service.channel;

import com.ruralhealthcare.medicine.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "medicine.notification.email-provider", havingValue = "gmail")
public class GmailSmtpEmailSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(GmailSmtpEmailSender.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public GmailSmtpEmailSender(JavaMailSender mailSender,
                                @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(String recipient, String message) {
        if (recipient == null || recipient.isBlank() || !EMAIL_PATTERN.matcher(recipient.trim()).matches()) {
            log.warn("Caregiver notification rejected: invalid recipient email [{}]", maskEmail(recipient));
            return DeliveryResult.failed("Invalid recipient email address");
        }

        String trimmedRecipient = recipient.trim();
        String maskedRecipient = maskEmail(trimmedRecipient);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                mailMessage.setFrom(fromAddress);
            }
            mailMessage.setTo(trimmedRecipient);
            mailMessage.setSubject("Aarogya Seva: Medicine Reminder Alert");
            mailMessage.setText(buildEmailBody(message));

            mailSender.send(mailMessage);

            String providerRef = "GMAIL-SMTP-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("📧 Caregiver notification email dispatched successfully to recipient [{}] (Ref: {})",
                    maskedRecipient, providerRef);
            return DeliveryResult.success(providerRef);
        } catch (Exception e) {
            log.error("Failed to dispatch caregiver email via Gmail SMTP to recipient [{}]: {}",
                    maskedRecipient, e.getMessage());
            return DeliveryResult.failed("SMTP delivery failed: " + e.getMessage());
        }
    }

    private String buildEmailBody(String message) {
        return "Hello,\n\n"
                + message + "\n\n"
                + "Please check in with the patient to support their medication schedule.\n\n"
                + "Regards,\n"
                + "Aarogya Seva Healthcare Team";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedName = name.length() > 2 ? name.substring(0, 2) + "***" : "***";
        return maskedName + domain;
    }
}
