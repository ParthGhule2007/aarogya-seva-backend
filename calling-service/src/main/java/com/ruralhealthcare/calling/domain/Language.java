package com.ruralhealthcare.calling.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    EN("en", "English"),
    HI("hi", "Hindi"),
    MR("mr", "Marathi");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Language fromCode(String value) {
        if (value == null || value.isBlank()) {
            return EN;
        }
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(value.trim()) || lang.name().equalsIgnoreCase(value.trim())) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported language code: " + value + ". Supported languages: en, hi, mr");
    }
}
