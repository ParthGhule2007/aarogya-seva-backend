package com.ruralhealthcare.medicine.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruralhealthcare.medicine.dto.OcrExtractionResponse;
import com.ruralhealthcare.medicine.service.OcrService;
import com.ruralhealthcare.medicine.util.ImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MockOcrServiceImpl implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(MockOcrServiceImpl.class);

    @Value("${medicine.ocr.provider:ocrspace}")
    private String provider;

    @Value("${medicine.ocr.api-key:helloworld}")
    private String apiKey;

    // Noise keywords on medicine cartons/blister packs to ignore when detecting brand names
    private static final Pattern NOISE_PATTERN = Pattern.compile(
            "(?i)\\b(rx|batch|mfg|exp|m\\.r\\.p|mrp|inclusive|marketed\\s+by|manufactured\\s+by|" +
            "keep\\s+out|store\\s+in|lic\\.?\\s*no|schedule\\s+h|caution|warning|physician|" +
            "film\\s+coated|enteric\\s+coated|excipients|dosage|contains|direction|retail)\\b"
    );

    private static final Pattern STRENGTH_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+(?:\\.\\d+)?\\s*(?:mg|ml|mcg|g|iu|%|IU))\\b"
    );

    private static final Pattern FORM_CLEANUP_PATTERN = Pattern.compile(
            "(?i)\\b(tablets?\\s+ip|capsules?\\s+ip|oral\\s+suspension|suspension|tablets?|capsules?|syrup|ointment|drops?|injection|gel|cream)\\b"
    );

    @Override
    public OcrExtractionResponse extractText(MultipartFile image) {
        ImageValidator.validateImage(image);

        log.info("Processing OCR for medicine packaging [{}] (size={} bytes) via provider [{}]...",
                image.getOriginalFilename(), image.getSize(), provider);

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("apikey", apiKey != null && !apiKey.isBlank() ? apiKey : "helloworld");
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("detectOrientation", "true");
            body.add("scale", "true");
            body.add("file", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename() != null ? image.getOriginalFilename() : "packaging.jpg";
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(8000);
            factory.setReadTimeout(15000);

            RestTemplate restTemplate = new RestTemplate(factory);
            JsonNode root = restTemplate.postForObject("https://api.ocr.space/parse/image", requestEntity, JsonNode.class);

            if (root != null) {
                if (root.has("IsErroredOnProcessing") && root.get("IsErroredOnProcessing").asBoolean()) {
                    String errMsg = root.has("ErrorMessage") ? root.get("ErrorMessage").asText() : "OCR Processing Error";
                    log.warn("OCR API returned processing error: {}", errMsg);
                    return new OcrExtractionResponse(
                            null, null, null,
                            "OCR processing error: " + errMsg,
                            0.0, false, "OCR_SPACE",
                            errMsg, Collections.emptyList()
                    );
                }

                if (root.has("ParsedResults") && root.get("ParsedResults").isArray() && !root.get("ParsedResults").isEmpty()) {
                    JsonNode firstResult = root.get("ParsedResults").get(0);
                    if (firstResult.has("ParsedText")) {
                        String fullText = firstResult.get("ParsedText").asText();
                        if (fullText != null && !fullText.isBlank()) {
                            return parseMedicinePackagingText(fullText.trim());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Online OCR processing error: {}. Falling back to manual entry mode.", ex.getMessage());
        }

        return new OcrExtractionResponse(
                null, null, null,
                "No readable medicine text detected from packaging photo. Please ensure clear lighting and enter medicine name manually.",
                0.0, false, "OCR_SPACE",
                "Could not detect clear text on packaging",
                Collections.emptyList()
        );
    }

    private OcrExtractionResponse parseMedicinePackagingText(String cleaned) {
        String[] lines = cleaned.split("[\\r\\n]+");
        List<String> candidates = new ArrayList<>();
        List<String> cleanCandidates = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() >= 3 && !trimmed.matches("^[0-9]+$")) {
                candidates.add(trimmed);
                if (!NOISE_PATTERN.matcher(trimmed).find()) {
                    cleanCandidates.add(trimmed);
                }
            }
        }

        String detectedBrand = null;
        String detectedStrength = null;
        double confidence = 0.50;

        // Pass 1: Look for strength in non-noise lines
        for (String cand : cleanCandidates) {
            Matcher sm = STRENGTH_PATTERN.matcher(cand);
            if (sm.find()) {
                detectedStrength = sm.group(1).trim();
                String rawBrand = cand.substring(0, sm.start()).trim();
                if (rawBrand.isBlank()) {
                    rawBrand = cand.substring(sm.end()).trim();
                }
                rawBrand = FORM_CLEANUP_PATTERN.matcher(rawBrand).replaceAll("").trim();
                if (rawBrand.length() >= 3) {
                    detectedBrand = rawBrand;
                    confidence = 0.90;
                    break;
                }
            }
        }

        // Pass 2: If strength not found in cleanCandidates, search all candidates for strength
        if (detectedStrength == null) {
            for (String cand : candidates) {
                Matcher sm = STRENGTH_PATTERN.matcher(cand);
                if (sm.find()) {
                    detectedStrength = sm.group(1).trim();
                    break;
                }
            }
        }

        // Pass 3: Brand selection if not yet found
        if (detectedBrand == null && !cleanCandidates.isEmpty()) {
            for (String cand : cleanCandidates) {
                String stripped = FORM_CLEANUP_PATTERN.matcher(cand).replaceAll("").trim();
                if (stripped.length() >= 3 && stripped.matches(".*[a-zA-Z]{3,}.*")) {
                    detectedBrand = stripped;
                    confidence = (detectedStrength != null) ? 0.82 : 0.70;
                    break;
                }
            }
        }

        // Pass 4: Fallback brand to first non-empty line
        if (detectedBrand == null && !candidates.isEmpty()) {
            detectedBrand = FORM_CLEANUP_PATTERN.matcher(candidates.get(0)).replaceAll("").trim();
            if (detectedBrand.isBlank()) detectedBrand = candidates.get(0);
            confidence = 0.45;
        }

        String finalDetectedName;
        if (detectedBrand != null && detectedStrength != null) {
            finalDetectedName = detectedBrand + " " + detectedStrength;
        } else if (detectedBrand != null) {
            finalDetectedName = detectedBrand;
        } else {
            finalDetectedName = cleanCandidates.isEmpty() ? null : cleanCandidates.get(0);
        }

        log.info("OCR parsed medicine packaging: Brand=[{}], Strength=[{}], DetectedName=[{}], Confidence={}",
                detectedBrand, detectedStrength, finalDetectedName, confidence);

        return new OcrExtractionResponse(
                finalDetectedName,
                detectedBrand,
                detectedStrength,
                cleaned,
                confidence,
                false,
                "OCR_SPACE",
                "Recognized text from packaging image via OCR",
                cleanCandidates.isEmpty() ? candidates : cleanCandidates
        );
    }
}
