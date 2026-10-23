package com.ruralhealthcare.calling.service.twilio;

public record TwilioCallDetails(
        String status,
        Integer errorCode,
        String errorMessage
) {}
