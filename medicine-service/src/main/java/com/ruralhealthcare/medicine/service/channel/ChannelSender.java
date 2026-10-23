package com.ruralhealthcare.medicine.service.channel;

import com.ruralhealthcare.medicine.domain.NotificationChannel;

public interface ChannelSender {

    NotificationChannel getChannel();

    DeliveryResult send(String recipient, String message);
}
