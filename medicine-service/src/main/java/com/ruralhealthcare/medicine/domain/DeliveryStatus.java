package com.ruralhealthcare.medicine.domain;

public enum DeliveryStatus {
    DETECTED,
    ATTEMPTED,
    SIMULATED_DISPATCH,
    GATEWAY_ACCEPTED,
    CARRIER_DELIVERED,
    GATEWAY_FAILED
}
