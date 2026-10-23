package com.ruralhealthcare.medicine.service.channel;

import com.ruralhealthcare.medicine.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "medicine.notification.email-provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(String recipient, String message) {
        String masked = maskEmail(recipient);
        String refId = "SIMULATED-EMAIL-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("📧 [SIMULATED EMAIL] Dispatch to recipient [{}]: \"{}\" (Ref: {})", masked, message, refId);
        return DeliveryResult.simulated(refId);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****@****";
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedName = name.length() > 2 ? name.substring(0, 2) + "***" : "***";
        return maskedName + domain;
    }
}
