package com.ruralhealthcare.calling.service.ultravox;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UltravoxCallRequest {

    private String systemPrompt;
    private String model = "ultravox-v0.7";
    private String voice = "73527457-256b-4291-a36e-10128315db4b";
    private String languageHint = "en";
    private double temperature = 0.3;
    private Map<String, Object> medium = Map.of("twilio", Map.of());
    private List<Map<String, Object>> selectedTools;
    private Map<String, Object> vadSettings;

    public UltravoxCallRequest() {
    }

    public UltravoxCallRequest(String systemPrompt, String model, String voice, String languageHint,
                               double temperature, List<Map<String, Object>> selectedTools) {
        this(systemPrompt, model, voice, languageHint, temperature, selectedTools, null);
    }

    public UltravoxCallRequest(String systemPrompt, String model, String voice, String languageHint,
                               double temperature, List<Map<String, Object>> selectedTools,
                               Map<String, Object> vadSettings) {
        this.systemPrompt = systemPrompt;
        this.model = model != null ? model : "ultravox-v0.7";
        this.voice = voice != null ? voice : "73527457-256b-4291-a36e-10128315db4b";
        this.languageHint = languageHint != null ? languageHint : "en";
        this.temperature = temperature;
        this.medium = Map.of("twilio", Map.of());
        this.selectedTools = selectedTools;
        this.vadSettings = vadSettings;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getLanguageHint() {
        return languageHint;
    }

    public void setLanguageHint(String languageHint) {
        this.languageHint = languageHint;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public Map<String, Object> getMedium() {
        return medium;
    }

    public void setMedium(Map<String, Object> medium) {
        this.medium = medium;
    }

    public List<Map<String, Object>> getSelectedTools() {
        return selectedTools;
    }

    public void setSelectedTools(List<Map<String, Object>> selectedTools) {
        this.selectedTools = selectedTools;
    }

    public Map<String, Object> getVadSettings() {
        return vadSettings;
    }

    public void setVadSettings(Map<String, Object> vadSettings) {
        this.vadSettings = vadSettings;
    }
}
