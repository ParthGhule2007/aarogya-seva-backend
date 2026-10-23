package com.ruralhealthcare.medicine.service.channel;

import com.ruralhealthcare.medicine.domain.DeliveryStatus;

public class DeliveryResult {

    private final DeliveryStatus status;
    private final String providerReference;
    private final String errorMessage;

    public DeliveryResult(DeliveryStatus status, String providerReference, String errorMessage) {
        this.status = status;
        this.providerReference = providerReference;
        this.errorMessage = errorMessage;
    }

    public static DeliveryResult simulated(String providerReference) {
        return new DeliveryResult(DeliveryStatus.SIMULATED_DISPATCH, providerReference, null);
    }

    public static DeliveryResult success(String providerReference) {
        return new DeliveryResult(DeliveryStatus.GATEWAY_ACCEPTED, providerReference, null);
    }

    public static DeliveryResult failed(String errorMessage) {
        return new DeliveryResult(DeliveryStatus.GATEWAY_FAILED, null, errorMessage);
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
