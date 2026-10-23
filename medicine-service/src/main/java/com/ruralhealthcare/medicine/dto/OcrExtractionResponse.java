package com.ruralhealthcare.medicine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class OcrExtractionResponse {

    private String detectedName;
    private String brand;
    private String strength;
    private String rawExtractedText;
    private Double confidence;

    @JsonProperty("isMock")
    private boolean isMock;
    private String provider;
    private String disclaimer;
    private List<String> candidateNames = new ArrayList<>();

    public OcrExtractionResponse() {
    }

    public OcrExtractionResponse(String detectedName, String rawExtractedText, Double confidence,
                                 boolean isMock, String provider, String disclaimer, List<String> candidateNames) {
        this(detectedName, null, null, rawExtractedText, confidence, isMock, provider, disclaimer, candidateNames);
    }

    public OcrExtractionResponse(String detectedName, String brand, String strength, String rawExtractedText, Double confidence,
                                 boolean isMock, String provider, String disclaimer, List<String> candidateNames) {
        this.detectedName = detectedName;
        this.brand = brand;
        this.strength = strength;
        this.rawExtractedText = rawExtractedText;
        this.confidence = confidence;
        this.isMock = isMock;
        this.provider = provider;
        this.disclaimer = disclaimer;
        this.candidateNames = candidateNames != null ? candidateNames : new ArrayList<>();
    }

    public static OcrExtractionResponse mockResponse() {
        return new OcrExtractionResponse(
                null,
                "[SIMULATED OCR] Image received. Real text recognition requires an approved OCR provider (e.g. AWS Textract).",
                null,
                true,
                "MOCK_SIMULATED",
                "Simulated OCR mode active. Printed packaging text must be reviewed and entered manually.",
                new ArrayList<>()
        );
    }

    public String getDetectedName() {
        return detectedName;
    }

    public void setDetectedName(String detectedName) {
        this.detectedName = detectedName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getRawExtractedText() {
        return rawExtractedText;
    }

    public void setRawExtractedText(String rawExtractedText) {
        this.rawExtractedText = rawExtractedText;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    @JsonProperty("isMock")
    public boolean isMock() {
        return isMock;
    }

    public void setMock(boolean mock) {
        isMock = mock;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public List<String> getCandidateNames() {
        return candidateNames;
    }

    public void setCandidateNames(List<String> candidateNames) {
        this.candidateNames = candidateNames != null ? candidateNames : new ArrayList<>();
    }
}
