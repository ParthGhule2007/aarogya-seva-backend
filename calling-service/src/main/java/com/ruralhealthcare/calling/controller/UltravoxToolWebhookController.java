package com.ruralhealthcare.calling.controller;

import com.ruralhealthcare.calling.dto.HospitalSearchResult;
import com.ruralhealthcare.calling.service.hospital.HospitalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tools")
@Tag(name = "Ultravox AI Tools", description = "Webhooks triggered dynamically by Ultravox voice AI during active phone calls")
public class UltravoxToolWebhookController {

    private static final Logger log = LoggerFactory.getLogger(UltravoxToolWebhookController.class);

    private final HospitalSearchService hospitalSearchService;

    public UltravoxToolWebhookController(HospitalSearchService hospitalSearchService) {
        this.hospitalSearchService = hospitalSearchService;
    }

    @Operation(
            summary = "Tool: searchNearbyHospitals",
            description = "Called by the AI voice agent when a caller asks for nearby medical facilities."
    )
    @PostMapping("/search-hospitals")
    public ResponseEntity<HospitalSearchResult> searchNearbyHospitals(@RequestBody Map<String, Object> body) {
        Object locationObj = body != null ? body.get("location") : null;
        String location = locationObj != null ? locationObj.toString() : "";

        log.info("[UltravoxToolWebhookController] Received tool execution request for location: [{}]", location);
        HospitalSearchResult result = hospitalSearchService.searchNearbyHospitals(location);

        return ResponseEntity.ok(result);
    }
}
