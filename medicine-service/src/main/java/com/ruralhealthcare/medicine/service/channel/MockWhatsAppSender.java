package com.ruralhealthcare.medicine.service.channel;

import com.ruralhealthcare.medicine.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockWhatsAppSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppSender.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public DeliveryResult send(String recipient, String message) {
        String masked = maskContact(recipient);
        String refId = "SIMULATED-WA-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("💬 [SIMULATED WHATSAPP] Dispatch to recipient [{}]: \"{}\" (Ref: {})", masked, message, refId);
        return DeliveryResult.simulated(refId);
    }

    private String maskContact(String contact) {
        if (contact == null || contact.length() < 4) return "****";
        return contact.substring(0, 3) + "******" + contact.substring(contact.length() - 2);
    }
}
