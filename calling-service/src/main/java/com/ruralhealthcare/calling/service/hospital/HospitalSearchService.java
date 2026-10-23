package com.ruralhealthcare.calling.service.hospital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.dto.HospitalSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HospitalSearchService {

    private static final Logger log = LoggerFactory.getLogger(HospitalSearchService.class);

    private final CallingProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public HospitalSearchService(CallingProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this(properties, restClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, "AarogyaSevaHealthcareVoiceService/1.0 (healthcare-research)")
                .build(), objectMapper);
    }

    public HospitalSearchService(CallingProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Searches for verified healthcare facilities based on a patient-provided town name, PIN code, or coordinates.
     * Never invents or generates unverified hospital records.
     *
     * @param locationQuery Patient-provided location string (e.g., PIN code "411001", town "Pune", or "Alandi, Pune, Maharashtra, 412105")
     * @return HospitalSearchResult containing verified records or an honest explanation when none can be verified
     */
    public HospitalSearchResult searchNearbyHospitals(String locationQuery) {
        if (locationQuery == null || locationQuery.isBlank()) {
            return HospitalSearchResult.notFound("", "No location was provided. Please provide a town, city, or PIN code.", "Verified Registry");
        }

        String cleanedQuery = locationQuery.trim();
        log.info("[HospitalSearchService] Searching verified hospitals for location query: [{}]", cleanedQuery);

        String provider = properties.getHospitalSearch().getProvider();

        if ("mock".equalsIgnoreCase(provider)) {
            return handleMockSearch(cleanedQuery);
        }

        if ("osm".equalsIgnoreCase(provider)) {
            return searchViaOpenStreetMap(cleanedQuery);
        }

        return searchHospitalsLive(cleanedQuery);
    }

    private HospitalSearchResult handleMockSearch(String query) {
        String lower = query.toLowerCase().trim();
        if (lower.contains("nowhere") || lower.equals("000000")) {
            return HospitalSearchResult.notFound(query, "No verified hospitals found within search radius. Please dial 108/112 for emergency dispatch.", "MockProvider");
        }

        // Test ambiguity handling for Alandi without district or PIN
        if ((lower.equals("alandi") || lower.equals("alandi, maharashtra")) && !lower.contains("pune") && !lower.contains("412105") && !lower.contains("nanded")) {
            return HospitalSearchResult.notFound(query,
                    "Ambiguous location: Multiple places named '" + query + "' were found (Alandi in Pune district, PIN 412105 vs Alandi in Nanded district, PIN 431736). Please specify your district or 6-digit PIN code.",
                    "MockProvider");
        }

        if (lower.contains("alandi") && (lower.contains("pune") || lower.contains("412105") || lower.contains("khed"))) {
            List<HospitalSearchResult.HospitalItem> items = List.of(
                    new HospitalSearchResult.HospitalItem("Jaihind Multi & Super Speciality Hospital", "Near Markal Road, Alandi, Pune", 1.8, "020-27185000", "108", "Hospital", 18.6750, 73.8960),
                    new HospitalSearchResult.HospitalItem("Aavati Hospital", "Dehu Phata, Alandi, Pune", 2.4, "020-27186000", "108", "Hospital", 18.6790, 73.8940),
                    new HospitalSearchResult.HospitalItem("Rural Hospital Alandi Devachi", "Wadmukhwadi Road, Alandi, Pune", 3.1, "020-27187000", "108", "Rural Hospital", 18.6710, 73.9020)
            );
            return HospitalSearchResult.success("Alandi, Pune, Maharashtra", items, "MockProvider");
        }

        List<HospitalSearchResult.HospitalItem> items = List.of(
                new HospitalSearchResult.HospitalItem("District Civil Hospital", "Main Road, Civil Lines", 3.2, "020-26127000", "108", "District Hospital", 18.5204, 73.8567),
                new HospitalSearchResult.HospitalItem("Community Health Centre (CHC)", "Station Road", 7.5, "020-25531000", "108", "Community Health Centre", 18.5314, 73.8446)
        );
        return HospitalSearchResult.success(query, items, "MockProvider");
    }

    /**
     * Queries OpenStreetMap Nominatim with explicit error checking for rate limits (429), server errors (5xx),
     * and network timeouts.
     */
    public JsonNode queryNominatim(String query) throws GeocodingException {
        String nominatimUrl = properties.getHospitalSearch().getNominatimUrl() +
                "/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) +
                "&format=json&limit=10&countrycodes=in&addressdetails=1";

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(java.net.URI.create(nominatimUrl))
                    .retrieve()
                    .toEntity(String.class);

            HttpStatusCode status = response.getStatusCode();
            if (status.value() == 429) {
                log.warn("[HospitalSearchService] Nominatim rate limit exceeded (HTTP 429) for query: [{}]", query);
                throw new GeocodingException("Geocoding service rate limit exceeded (HTTP 429). Usage policy requires at most 1 request per second.", 429, true, false, null);
            }
            if (status.is5xxServerError()) {
                log.warn("[HospitalSearchService] Nominatim server error (HTTP {}) for query: [{}]", status.value(), query);
                throw new GeocodingException("Geocoding service server error (HTTP " + status.value() + ").", status.value(), false, false, null);
            }

            String body = response.getBody();
            if (body == null || body.isBlank() || body.trim().equals("[]")) {
                return null;
            }
            return objectMapper.readTree(body);

        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Nominatim client error {}: {}", code, ex.getResponseBodyAsString());
            if (code == 429) {
                throw new GeocodingException("Geocoding service rate limit exceeded (HTTP 429). Usage policy requires at most 1 request per second.", 429, true, false, ex);
            }
            throw new GeocodingException("Geocoding service error (HTTP " + code + ")", code, false, false, ex);
        } catch (HttpServerErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Nominatim server error {}: {}", code, ex.getResponseBodyAsString());
            throw new GeocodingException("Geocoding service is temporarily unavailable (HTTP " + code + ")", code, false, false, ex);
        } catch (ResourceAccessException ex) {
            log.warn("[HospitalSearchService] Nominatim request timed out or network error: {}", ex.getMessage());
            throw new GeocodingException("Geocoding service request timed out.", 504, false, true, ex);
        } catch (JsonProcessingException ex) {
            log.warn("[HospitalSearchService] Failed to parse Nominatim JSON response: {}", ex.getMessage());
            throw new GeocodingException("Invalid response received from geocoding service.", 502, false, false, ex);
        }
    }

    /**
     * Generates a sensible, prioritized sequence of geocoding fallback queries from a raw location string.
     * Covers normalized town queries, sub-district (Khed/Haveli), town with PIN, and PIN-only queries.
     */
    public List<String> buildGeocodingQueries(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }

        String cleaned = rawQuery.trim().replaceAll("[.?!]+$", "");
        List<String> queries = new ArrayList<>();
        // Priority 1: Exact raw query as supplied by user
        queries.add(cleaned);

        // Extract 6-digit Indian PIN code
        Matcher pinMatcher = Pattern.compile("\\b([1-9][0-9]{5})\\b").matcher(cleaned);
        String pin = pinMatcher.find() ? pinMatcher.group(1) : null;

        String withoutPin = cleaned;
        if (pin != null) {
            withoutPin = withoutPin.replaceAll("\\b" + pin + "\\b", "")
                    .replaceAll(",\\s*,", ",")
                    .replaceAll("^,\\s*|\\s*,\\s*$", "")
                    .trim();
        }

        String[] parts = Arrays.stream(withoutPin.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);

        String town = parts.length > 0 ? parts[0] : "";
        String district = parts.length > 1 ? parts[1] : "";
        String state = parts.length > 2 ? parts[2] : "";

        // If town & district present
        if (!town.isBlank() && !district.isBlank()) {
            String stateStr = !state.isBlank() ? ", " + state : "";
            // Sensible normalized query: "Alandi, Pune, Maharashtra, India"
            queries.add(town + ", " + district + stateStr + ", India");

            // Sub-district / Taluka heuristic (e.g., Alandi in Pune is in Khed taluka)
            if ("alandi".equalsIgnoreCase(town) && "pune".equalsIgnoreCase(district)) {
                queries.add("Alandi, Khed, Pune, Maharashtra, India");
                queries.add("Alandi, Khed, Pune, India");
            }

            queries.add(town + ", " + district + stateStr);
            queries.add(town + ", " + district + ", India");
            queries.add(town + ", " + district);
        } else if (!withoutPin.isBlank()) {
            queries.add(withoutPin + ", India");
            queries.add(withoutPin);
        }

        // Add PIN-based queries
        if (pin != null) {
            if (!town.isBlank()) {
                queries.add(town + " " + pin + ", India");
                queries.add(town + " " + pin);
            }
            if (!state.isBlank()) {
                queries.add(pin + ", " + state + ", India");
            }
            queries.add(pin + ", India");
            queries.add(pin);
        }

        // Deduplicate while preserving order
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String q : queries) {
            String trimmed = q.trim();
            if (!trimmed.isBlank() && seen.add(trimmed.toLowerCase())) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static final Set<String> NON_DISTRICT_NAMES = Set.of(
            "india", "bharat",
            "maharashtra", "gujarat", "goa", "karnataka", "madhya pradesh",
            "rajasthan", "tamil nadu", "kerala", "andhra pradesh", "telangana",
            "uttar pradesh", "bihar", "west bengal", "punjab", "haryana",
            "delhi", "odisha", "jharkhand", "chhattisgarh", "assam",
            "himachal pradesh", "uttarakhand", "jammu and kashmir", "ladakh",
            "state"
    );

    /**
     * Extracts the intended district from user input to reject candidates from unrelated districts.
     */
    public String extractIntendedDistrict(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String lower = query.toLowerCase();
        if (lower.contains("pune")) {
            return "Pune";
        }
        if (lower.contains("nanded")) {
            return "Nanded";
        }
        if (lower.contains("nashik")) {
            return "Nashik";
        }
        if (lower.contains("mumbai")) {
            return "Mumbai";
        }
        if (lower.contains("nagpur")) {
            return "Nagpur";
        }
        if (lower.contains("aurangabad") || lower.contains("chhatrapati sambhajinagar")) {
            return "Chhatrapati Sambhajinagar";
        }

        // PIN 412105 is known for Pune district (Alandi/Haveli/Khed)
        if (lower.contains("412105")) {
            return "Pune";
        }

        String[] parts = query.split(",");
        for (int i = 1; i < parts.length; i++) {
            String cand = parts[i].trim();
            if (!cand.isBlank() && !cand.matches(".*\\d{6}.*")) {
                String candLower = cand.toLowerCase().replaceAll("[^a-z ]", "").trim();
                if (!NON_DISTRICT_NAMES.contains(candLower)) {
                    return cand;
                }
            }
        }
        return null;
    }

    public TargetLocationResult resolveTargetLocation(String query) {
        String targetDistrict = extractIntendedDistrict(query);
        List<String> fallbackQueries = buildGeocodingQueries(query);

        SettlementCandidate selectedSettlement = null;
        SettlementCandidate supportingPostalArea = null;
        SettlementResolution lastResolution = null;

        for (String q : fallbackQueries) {
            try {
                JsonNode geoArray = queryNominatim(q);
                if (geoArray == null || !geoArray.isArray() || geoArray.isEmpty()) {
                    continue;
                }

                SettlementResolution resolution = resolveSettlement(geoArray, q, targetDistrict);
                if (resolution.isAmbiguous()) {
                    return new TargetLocationResult(HospitalSearchResult.notFound(query, resolution.getMessage(), "OpenStreetMap Nominatim"));
                }

                SettlementCandidate candidate = resolution.getSelected();
                if (candidate != null) {
                    if (candidate.isSettlement) {
                        selectedSettlement = candidate;
                        log.info("[HospitalSearchService] Successfully geocoded [{}] to populated settlement [{}] in [{}] via query [{}]",
                                query, candidate.displayName, candidate.district, q);
                        break;
                    } else if (candidate.isPostalArea && supportingPostalArea == null) {
                        supportingPostalArea = candidate;
                        log.info("[HospitalSearchService] Found supporting postal area [{}] via query [{}]",
                                candidate.postcode, q);
                    }
                }
                lastResolution = resolution;
            } catch (GeocodingException ex) {
                log.warn("[HospitalSearchService] Geocoding exception during query [{}]: {}", q, ex.getMessage());
                if (ex.isRateLimited()) {
                    return new TargetLocationResult(HospitalSearchResult.notFound(query,
                            "Geocoding service rate limit exceeded (HTTP 429). Please wait a moment before retrying, or dial 108/112 in an emergency.",
                            "OpenStreetMap Nominatim"));
                } else if (ex.isTimedOut()) {
                    return new TargetLocationResult(HospitalSearchResult.notFound(query,
                            "Geocoding service request timed out. Please try again shortly or dial 108/112 in an emergency.",
                            "OpenStreetMap Nominatim"));
                } else if (ex.getStatusCode() >= 500) {
                    return new TargetLocationResult(HospitalSearchResult.notFound(query,
                            "Geocoding service is temporarily unavailable (HTTP " + ex.getStatusCode() + "). Please try again shortly or dial 108/112 for emergency dispatch.",
                            "OpenStreetMap Nominatim"));
                }
            }
        }

        SettlementCandidate targetLocation;
        String resolvedLocationName;

        if (selectedSettlement != null) {
            targetLocation = selectedSettlement;
            String dist = selectedSettlement.district;
            if (!dist.isBlank() && !query.toLowerCase().contains(dist.toLowerCase())) {
                resolvedLocationName = query + ", " + dist;
            } else {
                resolvedLocationName = query;
            }
        } else if (supportingPostalArea != null) {
            // Requirement 4: If the PIN code resolves to a postal area rather than a settlement,
            // use it as supporting location evidence, not as proof that a particular settlement was found.
            targetLocation = supportingPostalArea;
            resolvedLocationName = query + " (Postal Area PIN " + supportingPostalArea.postcode +
                    (!supportingPostalArea.district.isBlank() ? ", " + supportingPostalArea.district : "") + ")";
            log.info("[HospitalSearchService] Using postal area PIN {} as supporting location evidence at [lat={}, lon={}]",
                    supportingPostalArea.postcode, targetLocation.lat, targetLocation.lon);
        } else {
            String msg = (lastResolution != null && lastResolution.getMessage() != null)
                    ? lastResolution.getMessage()
                    : "Could not verify geographic coordinates for '" + query + "'. Please check the town name or PIN code.";
            return new TargetLocationResult(HospitalSearchResult.notFound(query, msg, "OpenStreetMap Nominatim"));
        }

        return new TargetLocationResult(targetLocation, resolvedLocationName);
    }

    public HospitalSearchResult searchHospitalsLive(String query) {
        TargetLocationResult locationResult = resolveTargetLocation(query);
        if (locationResult.getNotFoundResult() != null) {
            return locationResult.getNotFoundResult();
        }

        double lat = locationResult.getLat();
        double lon = locationResult.getLon();
        String resolvedLocationName = locationResult.getResolvedLocationName();
        log.info("[HospitalSearchService] Resolved coordinates for [{}]: [lat={}, lon={}]", resolvedLocationName, lat, lon);

        // 1. Primary provider: Geoapify Places API (if configured and enabled)
        CallingProperties.HospitalSearchProperties.GeoapifyProperties geoProps = properties.getHospitalSearch().getGeoapify();
        String apiKey = geoProps != null ? geoProps.getApiKey() : null;

        if (apiKey != null && !apiKey.isBlank()) {
            GeoapifyResponse geoResp = searchViaGeoapify(lat, lon, resolvedLocationName);
            if (geoResp.isSuccess() && !geoResp.getHospitals().isEmpty()) {
                String successMsg = "Found " + geoResp.getHospitals().size() +
                        " verified healthcare facility(ies) within " + (int) geoResp.getRadiusUsedKm() + " km of " + resolvedLocationName + ".";
                return HospitalSearchResult.success(resolvedLocationName, successMsg, geoResp.getHospitals(), "Geoapify Places", geoResp.getRadiusUsedKm());
            }
            log.info("[HospitalSearchService] Geoapify Places returned [{}] (status: {}). Falling back to OpenStreetMap Overpass...",
                    geoResp.getMessage(), geoResp.getStatus());
        } else {
            log.info("[HospitalSearchService] Geoapify API key not configured. Using OpenStreetMap Overpass fallback...");
        }

        // 2. Fallback provider: OpenStreetMap Overpass API
        return searchViaOverpass(lat, lon, resolvedLocationName);
    }

    public GeoapifyResponse searchViaGeoapify(double lat, double lon, String resolvedLocationName) {
        CallingProperties.HospitalSearchProperties.GeoapifyProperties geoProps = properties.getHospitalSearch().getGeoapify();
        String apiKey = geoProps != null ? geoProps.getApiKey() : null;
        if (apiKey == null || apiKey.isBlank()) {
            return GeoapifyResponse.invalid(0.0, "Geoapify API key not configured");
        }

        double initialRadiusKm = geoProps.getInitialRadiusKm();
        double expandedRadiusKm = geoProps.getExpandedRadiusKm();
        int minResultsToExpand = geoProps.getMinResultsToExpand();
        int maxResults = properties.getHospitalSearch().getMaxResults();

        // Step A: Search initial nearby radius (e.g. 10 km)
        log.info("[HospitalSearchService] Querying Geoapify Places for [{}] with initial radius {} km",
                resolvedLocationName, initialRadiusKm);
        GeoapifyResponse initialResp = queryGeoapifyEndpoint(lat, lon, initialRadiusKm, resolvedLocationName);

        if (!initialResp.isSuccess() && initialResp.getStatus() != GeoapifyResponse.Status.EMPTY) {
            // Error occurred (timeout, 429, 5xx, etc.) -> return immediately to trigger OSM fallback
            return initialResp;
        }

        List<HospitalSearchResult.HospitalItem> items = new ArrayList<>(initialResp.getHospitals());
        double radiusUsed = initialRadiusKm;

        // Step B: Expand radius if few results found (e.g. < 2)
        if (items.size() < minResultsToExpand && expandedRadiusKm > initialRadiusKm) {
            log.info("[HospitalSearchService] Found {} healthcare facility(ies) in initial radius ({} km). Expanding search radius to {} km...",
                    items.size(), initialRadiusKm, expandedRadiusKm);
            GeoapifyResponse expandedResp = queryGeoapifyEndpoint(lat, lon, expandedRadiusKm, resolvedLocationName);
            if (expandedResp.isSuccess()) {
                radiusUsed = expandedRadiusKm;
                Set<String> seen = new java.util.HashSet<>();
                for (HospitalSearchResult.HospitalItem h : items) {
                    seen.add(h.getName().trim().toLowerCase());
                }
                for (HospitalSearchResult.HospitalItem h : expandedResp.getHospitals()) {
                    if (seen.add(h.getName().trim().toLowerCase())) {
                        items.add(h);
                    }
                }
            } else if (items.isEmpty()) {
                return expandedResp;
            }
        }

        items.sort(java.util.Comparator.comparingDouble(HospitalSearchResult.HospitalItem::getDistanceKm));
        if (items.size() > maxResults) {
            items = items.subList(0, maxResults);
        }

        if (items.isEmpty()) {
            return GeoapifyResponse.empty(radiusUsed, "No verified healthcare facilities found within " + (int) radiusUsed + " km of " + resolvedLocationName);
        }

        return GeoapifyResponse.success(items, radiusUsed);
    }

    public GeoapifyResponse queryGeoapifyEndpoint(double lat, double lon, double radiusKm, String resolvedLocationName) {
        CallingProperties.HospitalSearchProperties.GeoapifyProperties geoProps = properties.getHospitalSearch().getGeoapify();
        String apiKey = geoProps.getApiKey();
        String apiUrl = geoProps.getApiUrl();
        String categories = geoProps.getCategories();
        int limit = geoProps.getLimit();
        int radiusMeters = (int) (radiusKm * 1000);

        String url = String.format(
                java.util.Locale.ROOT,
                "%s?categories=%s&filter=circle:%.6f,%.6f,%d&bias=proximity:%.6f,%.6f&limit=%d&apiKey=%s",
                apiUrl,
                categories,
                lon, lat, radiusMeters,
                lon, lat,
                limit,
                apiKey
        );

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(java.net.URI.create(url))
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .toEntity(String.class);

            HttpStatusCode statusCode = response.getStatusCode();
            int code = statusCode.value();

            if (code == 429) {
                log.warn("[HospitalSearchService] Geoapify rate limit exceeded (HTTP 429)");
                return GeoapifyResponse.rateLimited(code, radiusKm, "Geoapify rate limit exceeded (HTTP 429).");
            }
            if (statusCode.is5xxServerError()) {
                log.warn("[HospitalSearchService] Geoapify server error (HTTP {})", code);
                return GeoapifyResponse.serverError(code, radiusKm, "Geoapify server error (HTTP " + code + ").");
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return GeoapifyResponse.empty(radiusKm, "Empty response received from Geoapify.");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (features.isMissingNode() || !features.isArray()) {
                return GeoapifyResponse.invalid(radiusKm, "Geoapify response missing 'features' array.");
            }

            if (features.isEmpty()) {
                return GeoapifyResponse.empty(radiusKm, "No features found in search radius.");
            }

            List<HospitalSearchResult.HospitalItem> hospitalList = parseGeoapifyFeatures(features, lat, lon, resolvedLocationName, radiusKm);
            if (hospitalList.isEmpty()) {
                return GeoapifyResponse.empty(radiusKm, "No named hospital/healthcare records found in features.");
            }

            return GeoapifyResponse.success(hospitalList, radiusKm);

        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Geoapify client error {}: {}", code, getSanitizedSnippet(ex.getResponseBodyAsString()));
            if (code == 429) {
                return GeoapifyResponse.rateLimited(code, radiusKm, "Geoapify rate limit exceeded (HTTP 429).");
            }
            if (code == 401 || code == 403) {
                return GeoapifyResponse.clientError(code, radiusKm, "Geoapify authentication error (HTTP " + code + ").");
            }
            return GeoapifyResponse.clientError(code, radiusKm, "Geoapify client error (HTTP " + code + ").");
        } catch (HttpServerErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Geoapify server error {}: {}", code, getSanitizedSnippet(ex.getResponseBodyAsString()));
            return GeoapifyResponse.serverError(code, radiusKm, "Geoapify server error (HTTP " + code + ").");
        } catch (ResourceAccessException ex) {
            log.warn("[HospitalSearchService] Geoapify request timed out or network error: {}", ex.getMessage());
            return GeoapifyResponse.timedOut(504, radiusKm, "Geoapify request timed out.");
        } catch (JsonProcessingException ex) {
            log.warn("[HospitalSearchService] Failed to parse Geoapify JSON: {}", ex.getMessage());
            return GeoapifyResponse.invalid(radiusKm, "Invalid JSON received from Geoapify.");
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.warn("[HospitalSearchService] Geoapify connection/timeout error: {}", ex.getMessage());
                return GeoapifyResponse.timedOut(504, radiusKm, "Geoapify connection timed out.");
            }
            log.warn("[HospitalSearchService] Unexpected error querying Geoapify: {}", ex.getMessage());
            return GeoapifyResponse.serverError(500, radiusKm, "Unexpected Geoapify error: " + ex.getClass().getSimpleName());
        }
    }

    public List<HospitalSearchResult.HospitalItem> parseGeoapifyFeatures(
            JsonNode features, double callerLat, double callerLon, String resolvedLocationName, double radiusKm) {
        List<HospitalSearchResult.HospitalItem> list = new ArrayList<>();
        if (features == null || !features.isArray()) {
            return list;
        }

        Set<String> seenKeys = new java.util.HashSet<>();

        for (JsonNode feature : features) {
            JsonNode props = feature.path("properties");

            // Extract name
            String name = props.path("name").asText("");
            if (name.isBlank()) {
                name = props.path("name:en").asText("");
            }
            if (name.isBlank()) {
                String line1 = props.path("address_line1").asText("");
                if (!line1.isBlank() && !line1.matches("^\\d+.*")) {
                    name = line1;
                }
            }
            // Reject anonymous / unnamed records
            if (name.isBlank()) {
                continue;
            }

            // Extract coordinates
            Double hospitalLat = null;
            Double hospitalLon = null;
            if (props.has("lat") && props.has("lon")) {
                hospitalLat = props.get("lat").asDouble();
                hospitalLon = props.get("lon").asDouble();
            } else {
                JsonNode coords = feature.path("geometry").path("coordinates");
                if (coords.isArray() && coords.size() >= 2) {
                    hospitalLon = coords.get(0).asDouble();
                    hospitalLat = coords.get(1).asDouble();
                }
            }

            if (hospitalLat == null || hospitalLon == null ||
                    (hospitalLat == 0.0 && hospitalLon == 0.0) ||
                    hospitalLat < -90.0 || hospitalLat > 90.0 ||
                    hospitalLon < -180.0 || hospitalLon > 180.0) {
                continue;
            }

            double distanceKm = calculateHaversineKm(callerLat, callerLon, hospitalLat, hospitalLon);
            if (distanceKm > radiusKm) {
                continue; // Outside radius
            }

            // Deduplication
            String normName = name.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String dedupKey = normName + "@" + (Math.round(hospitalLat * 100.0) / 100.0) + "," + (Math.round(hospitalLon * 100.0) / 100.0);
            if (seenKeys.contains(dedupKey) || seenKeys.contains(normName)) {
                continue;
            }
            seenKeys.add(dedupKey);
            seenKeys.add(normName);

            // Address construction
            String formatted = props.path("formatted").asText("");
            String line2 = props.path("address_line2").asText("");
            String street = props.path("street").asText("");
            String city = props.path("city").asText(props.path("suburb").asText(""));
            String district = props.path("district").asText("");

            String address;
            if (!formatted.isBlank()) {
                address = formatted;
            } else if (!line2.isBlank()) {
                address = line2;
            } else if (!street.isBlank() && !city.isBlank()) {
                address = street + ", " + city + (!district.isBlank() ? ", " + district : "");
            } else if (!city.isBlank()) {
                address = city + (!district.isBlank() ? ", " + district : "");
            } else {
                address = resolvedLocationName;
            }

            // Phone numbers
            String phone = props.path("datasource").path("raw").path("phone").asText("");
            if (phone.isBlank()) {
                phone = props.path("contact").path("phone").asText("");
            }
            if (phone.isBlank()) {
                phone = props.path("datasource").path("raw").path("contact:phone").asText("");
            }

            String emergency = props.path("datasource").path("raw").path("emergency:phone").asText("");
            if (emergency.isBlank()) {
                emergency = !phone.isBlank() ? phone : "108";
            }

            // Facility Type
            String facilityType = "Hospital";
            JsonNode categoriesNode = props.path("categories");
            if (categoriesNode.isArray()) {
                boolean isClinic = false;
                boolean isHospital = false;
                for (JsonNode cat : categoriesNode) {
                    String catStr = cat.asText("");
                    if (catStr.startsWith("healthcare.hospital")) {
                        isHospital = true;
                    } else if (catStr.startsWith("healthcare.clinic")) {
                        isClinic = true;
                    }
                }
                if (isHospital) {
                    facilityType = "Hospital";
                } else if (isClinic) {
                    facilityType = "Clinic / Healthcare Centre";
                }
            }
            if ("Hospital".equals(facilityType) && props.path("datasource").path("raw").has("healthcare")) {
                facilityType = props.path("datasource").path("raw").path("healthcare").asText("Hospital");
            }

            list.add(new HospitalSearchResult.HospitalItem(
                    name,
                    address,
                    Math.round(distanceKm * 10.0) / 10.0,
                    phone,
                    emergency,
                    facilityType,
                    hospitalLat,
                    hospitalLon
            ));
        }

        // Sort by distance ascending
        list.sort(java.util.Comparator.comparingDouble(HospitalSearchResult.HospitalItem::getDistanceKm));
        return list;
    }

    public HospitalSearchResult searchViaOpenStreetMap(String query) {
        TargetLocationResult locationResult = resolveTargetLocation(query);
        if (locationResult.getNotFoundResult() != null) {
            return locationResult.getNotFoundResult();
        }
        return searchViaOverpass(locationResult.getLat(), locationResult.getLon(), locationResult.getResolvedLocationName());
    }

    public HospitalSearchResult searchViaOverpass(double lat, double lon, String resolvedLocationName) {
        OverpassResponse overpassResp = fetchHospitalsWithRetry(lat, lon);
        double radiusUsedKm = overpassResp.getRadiusUsedKm();

        if (overpassResp.getStatus() == OverpassResponse.Status.RATE_LIMITED) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "Hospital directory service rate limit exceeded (HTTP 429). Please wait a moment before retrying, or dial 108/112 in an emergency.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        if (overpassResp.getStatus() == OverpassResponse.Status.TIMED_OUT) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "Verified hospital directory search temporarily timed out for " + resolvedLocationName + ". In an emergency, please dial 108 or 112 immediately.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        if (overpassResp.getStatus() == OverpassResponse.Status.SERVER_ERROR) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "Verified hospital directory server is temporarily unavailable (HTTP " + overpassResp.getStatusCode() + ") for " + resolvedLocationName + ". In case of urgent symptoms, dial 108 or 112.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        if (overpassResp.getStatus() == OverpassResponse.Status.CLIENT_ERROR || overpassResp.getStatus() == OverpassResponse.Status.INVALID_RESPONSE) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "Verified hospital search is currently unavailable for " + resolvedLocationName + ". In case of urgent symptoms, dial national emergency ambulance at 108 or 112.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        if (overpassResp.getStatus() == OverpassResponse.Status.EMPTY || overpassResp.getElements() == null || overpassResp.getElements().isEmpty()) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "No verified hospitals recorded within " + (int) radiusUsedKm + " km of " + resolvedLocationName + ". Please dial 108/112 or visit your nearest district healthcare centre.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        List<HospitalSearchResult.HospitalItem> hospitalList = parseOverpassElements(overpassResp.getElements(), lat, lon, resolvedLocationName, radiusUsedKm);

        if (hospitalList.isEmpty()) {
            return HospitalSearchResult.notFound(resolvedLocationName,
                    "No verified hospitals recorded within " + (int) radiusUsedKm + " km of " + resolvedLocationName + ". Please dial 108/112 or visit your nearest district healthcare centre.",
                    "OpenStreetMap Overpass", radiusUsedKm);
        }

        String successMsg = "Found " + hospitalList.size() + " verified healthcare facility(ies) within " + (int) radiusUsedKm + " km of " + resolvedLocationName + ".";
        return HospitalSearchResult.success(resolvedLocationName, successMsg, hospitalList, "OpenStreetMap Registry", radiusUsedKm);
    }

    public SettlementResolution resolveSettlement(JsonNode geoArray, String query, String targetDistrict) {
        if (geoArray == null || !geoArray.isArray() || geoArray.isEmpty()) {
            return SettlementResolution.notFound("Could not resolve '" + query + "' to a known geographic area. Please specify the district or state.");
        }

        List<SettlementCandidate> settlements = new ArrayList<>();
        List<SettlementCandidate> postalAreas = new ArrayList<>();
        List<SettlementCandidate> otherCandidates = new ArrayList<>();

        for (JsonNode node : geoArray) {
            String cls = node.path("class").asText("");
            String typ = node.path("type").asText("");
            JsonNode addr = node.path("address");

            // 1. Reject waterways, rivers, streams, canals
            if ("waterway".equalsIgnoreCase(cls) || "river".equalsIgnoreCase(typ) || "stream".equalsIgnoreCase(typ) || "canal".equalsIgnoreCase(typ)) {
                log.debug("[HospitalSearchService] Skipping waterway candidate: {} ({})", node.path("display_name").asText(), typ);
                continue;
            }

            // 2. Reject natural landmarks that are not populated settlements
            if ("natural".equalsIgnoreCase(cls)) {
                log.debug("[HospitalSearchService] Skipping natural landmark: {}", node.path("display_name").asText());
                continue;
            }

            double lat = node.path("lat").asDouble();
            double lon = node.path("lon").asDouble();
            String displayName = node.path("display_name").asText("");

            String district = addr.path("state_district").asText("");
            if (district.isBlank()) {
                district = addr.path("district").asText("");
            }
            if (district.isBlank()) {
                district = addr.path("county").asText("");
            }
            String state = addr.path("state").asText("");
            String postcode = addr.path("postcode").asText("");

            // 3. Reject candidates from the wrong district when an intended district is specified
            if (targetDistrict != null && !targetDistrict.isBlank() && !district.isBlank()) {
                boolean matchesTarget = district.equalsIgnoreCase(targetDistrict) ||
                        addr.path("state_district").asText("").equalsIgnoreCase(targetDistrict) ||
                        addr.path("county").asText("").equalsIgnoreCase(targetDistrict);
                if (!matchesTarget) {
                    log.info("[HospitalSearchService] Rejecting candidate from wrong district [{}] (expected [{}])", district, targetDistrict);
                    continue;
                }
            }

            boolean isPostalArea = "postcode".equalsIgnoreCase(typ) ||
                    ("place".equalsIgnoreCase(cls) && "postcode".equalsIgnoreCase(typ)) ||
                    ("boundary".equalsIgnoreCase(cls) && "postal_code".equalsIgnoreCase(typ));

            boolean isSettlement = !isPostalArea && (
                    "place".equalsIgnoreCase(cls) ||
                    ("boundary".equalsIgnoreCase(cls) && "administrative".equalsIgnoreCase(typ)) ||
                    List.of("city", "town", "village", "hamlet", "suburb", "locality", "neighbourhood", "municipality", "administrative").contains(typ.toLowerCase())
            );

            SettlementCandidate candidate = new SettlementCandidate(lat, lon, displayName, district, state, postcode, isSettlement, isPostalArea);

            if (isSettlement) {
                settlements.add(candidate);
            } else if (isPostalArea) {
                postalAreas.add(candidate);
            } else {
                otherCandidates.add(candidate);
            }
        }

        if (settlements.isEmpty() && postalAreas.isEmpty() && otherCandidates.isEmpty()) {
            return SettlementResolution.notFound("No matching locations found in district '" + (targetDistrict != null ? targetDistrict : "intended") + "' for query '" + query + "'.");
        }

        // Prefer populated settlements
        if (!settlements.isEmpty()) {
            String queryLower = query.toLowerCase();

            // Check if user explicitly mentioned a district or PIN code
            List<SettlementCandidate> explicitMatches = settlements.stream()
                    .filter(c -> (!c.district.isBlank() && queryLower.contains(c.district.toLowerCase())) ||
                                 (!c.postcode.isBlank() && queryLower.contains(c.postcode)))
                    .toList();

            if (!explicitMatches.isEmpty()) {
                return SettlementResolution.selected(explicitMatches.get(0));
            }

            // Group candidates by district to detect cross-district ambiguity (when no targetDistrict was specified)
            if (targetDistrict == null || targetDistrict.isBlank()) {
                Map<String, List<SettlementCandidate>> byDistrict = new LinkedHashMap<>();
                for (SettlementCandidate c : settlements) {
                    String key = (!c.district.isBlank() ? c.district : "Area") + (!c.state.isBlank() ? ", " + c.state : "");
                    byDistrict.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
                }

                if (byDistrict.size() > 1) {
                    List<String> options = new ArrayList<>();
                    for (Map.Entry<String, List<SettlementCandidate>> entry : byDistrict.entrySet()) {
                        String pin = entry.getValue().get(0).postcode;
                        options.add(entry.getKey() + (!pin.isBlank() ? " (PIN " + pin + ")" : ""));
                    }
                    String optionsStr = String.join(" vs ", options);
                    return SettlementResolution.ambiguous(
                            "Ambiguous location: Multiple places named '" + query + "' were found (" + optionsStr +
                            "). Please specify your district or 6-digit PIN code so we can find verified hospitals near you."
                    );
                }
            }

            return SettlementResolution.selected(settlements.get(0));
        }

        // If no settlement is found, check if a postal area is available
        if (!postalAreas.isEmpty()) {
            return SettlementResolution.selected(postalAreas.get(0));
        }

        if (!otherCandidates.isEmpty()) {
            return SettlementResolution.selected(otherCandidates.get(0));
        }

        return SettlementResolution.notFound("Could not resolve an inhabited settlement for '" + query + "'. Please specify your district or state.");
    }

    public SettlementResolution resolveSettlement(JsonNode geoArray, String query) {
        return resolveSettlement(geoArray, query, extractIntendedDistrict(query));
    }

    public List<HospitalSearchResult.HospitalItem> parseOverpassElements(JsonNode elements, double callerLat, double callerLon, String resolvedLocationName) {
        return parseOverpassElements(elements, callerLat, callerLon, resolvedLocationName, properties.getHospitalSearch().getSearchRadiusKm());
    }

    public List<HospitalSearchResult.HospitalItem> parseOverpassElements(JsonNode elements, double callerLat, double callerLon, String resolvedLocationName, double searchRadiusKm) {
        List<HospitalSearchResult.HospitalItem> hospitalList = new ArrayList<>();
        if (elements == null || !elements.isArray()) {
            return hospitalList;
        }

        int maxResults = properties.getHospitalSearch().getMaxResults();
        Set<String> seenHospitalKeys = new java.util.HashSet<>();

        for (JsonNode elem : elements) {
            JsonNode tags = elem.path("tags");
            String name = tags.path("name").asText("");
            if (name.isBlank()) {
                name = tags.path("name:en").asText("");
            }
            if (name.isBlank()) {
                continue; // Reject records without valid names
            }

            Double hospitalLat = null;
            Double hospitalLon = null;
            if (elem.has("lat") && elem.has("lon")) {
                hospitalLat = elem.get("lat").asDouble();
                hospitalLon = elem.get("lon").asDouble();
            } else if (elem.has("center") && elem.path("center").has("lat") && elem.path("center").has("lon")) {
                hospitalLat = elem.path("center").get("lat").asDouble();
                hospitalLon = elem.path("center").get("lon").asDouble();
            }

            // Reject records without usable coordinates
            if (hospitalLat == null || hospitalLon == null ||
                    (hospitalLat == 0.0 && hospitalLon == 0.0) ||
                    hospitalLat < -90.0 || hospitalLat > 90.0 ||
                    hospitalLon < -180.0 || hospitalLon > 180.0) {
                continue;
            }

            double distanceKm = calculateHaversineKm(callerLat, callerLon, hospitalLat, hospitalLon);
            if (distanceKm > searchRadiusKm) {
                continue; // Exclude hospitals outside configured search radius
            }

            // Deduplicate by normalized name and approximate coordinates
            String normName = name.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String dedupKey = normName + "@" + (Math.round(hospitalLat * 100.0) / 100.0) + "," + (Math.round(hospitalLon * 100.0) / 100.0);
            if (seenHospitalKeys.contains(dedupKey) || seenHospitalKeys.contains(normName)) {
                continue; // Remove duplicate results
            }
            seenHospitalKeys.add(dedupKey);
            seenHospitalKeys.add(normName);

            String street = tags.path("addr:street").asText("");
            String city = tags.path("addr:city").asText("");
            String address = (!street.isBlank() && !city.isBlank()) ? street + ", " + city :
                             (!street.isBlank()) ? street :
                             (!city.isBlank()) ? city : resolvedLocationName;

            String phone = tags.path("phone").asText(tags.path("contact:phone").asText(""));
            String emergency = tags.path("emergency:phone").asText(phone.isBlank() ? "108" : phone);
            String facilityType = tags.path("healthcare").asText(tags.path("amenity").asText("Hospital"));

            hospitalList.add(new HospitalSearchResult.HospitalItem(
                    name,
                    address,
                    Math.round(distanceKm * 10.0) / 10.0,
                    phone,
                    emergency,
                    facilityType,
                    hospitalLat,
                    hospitalLon
            ));
        }

        // Sort by distance ascending
        hospitalList.sort(java.util.Comparator.comparingDouble(HospitalSearchResult.HospitalItem::getDistanceKm));
        if (hospitalList.size() > maxResults) {
            hospitalList = hospitalList.subList(0, maxResults);
        }

        return hospitalList;
    }

    public OverpassResponse fetchHospitalsWithRetry(double lat, double lon) {
        String primaryUrl = properties.getHospitalSearch().getOverpassUrl();
        String fallbackUrl = properties.getHospitalSearch().getOverpassFallbackUrl();
        double primaryRadius = properties.getHospitalSearch().getSearchRadiusKm();
        double fallbackRadius = properties.getHospitalSearch().getFallbackRadiusKm();

        // 1. Attempt primary endpoint with primary (broad) radius
        log.info("[HospitalSearchService] Querying primary Overpass [{}] with radius {} km", primaryUrl, primaryRadius);
        OverpassResponse primaryResp = queryOverpassEndpoint(primaryUrl, lat, lon, primaryRadius);

        if (primaryResp.isSuccess() || primaryResp.isEmpty()) {
            return primaryResp;
        }

        // 2. If broad search timed out, retry with smaller fallback radius on primary endpoint
        OverpassResponse retryResp = null;
        if (primaryResp.getStatus() == OverpassResponse.Status.TIMED_OUT && fallbackRadius < primaryRadius) {
            log.info("[HospitalSearchService] Broad search ({} km) timed out on primary. Retrying primary with smaller radius ({} km)",
                    primaryRadius, fallbackRadius);
            retryResp = queryOverpassEndpoint(primaryUrl, lat, lon, fallbackRadius);
            if (retryResp.isSuccess() || retryResp.isEmpty()) {
                return retryResp;
            }
        }

        // 3. If primary failed (timeout, 5xx, or 429), try fallback endpoint if configured
        if (fallbackUrl != null && !fallbackUrl.isBlank() && !fallbackUrl.equalsIgnoreCase(primaryUrl)) {
            double radiusForFallback = fallbackRadius;
            log.info("[HospitalSearchService] Trying fallback Overpass mirror [{}] with radius {} km", fallbackUrl, radiusForFallback);
            OverpassResponse fallbackResp = queryOverpassEndpoint(fallbackUrl, lat, lon, radiusForFallback);
            if (fallbackResp.isSuccess() || fallbackResp.isEmpty()) {
                return fallbackResp;
            }

            // Both endpoints failed: return the most specific meaningful error
            if (primaryResp.getStatus() == OverpassResponse.Status.RATE_LIMITED || fallbackResp.getStatus() == OverpassResponse.Status.RATE_LIMITED) {
                return fallbackResp.getStatus() == OverpassResponse.Status.RATE_LIMITED ? fallbackResp : primaryResp;
            }
            if (fallbackResp.getStatus() == OverpassResponse.Status.TIMED_OUT || primaryResp.getStatus() == OverpassResponse.Status.TIMED_OUT) {
                return fallbackResp.getStatus() == OverpassResponse.Status.TIMED_OUT ? fallbackResp : primaryResp;
            }
            return fallbackResp;
        }

        return retryResp != null ? retryResp : primaryResp;
    }

    public OverpassResponse queryOverpassEndpoint(String endpoint, double lat, double lon, double radiusKm) {
        if (endpoint == null || endpoint.isBlank()) {
            return OverpassResponse.invalid("", 400, radiusKm, "Endpoint URL is empty.");
        }

        int radiusMeters = (int) (radiusKm * 1000);
        int maxResults = properties.getHospitalSearch().getMaxResults();
        int timeoutSec = Math.max(5, properties.getHospitalSearch().getTimeoutMs() / 1000);

        // Optimized query: retrieve only point nodes and building ways; avoid heavy multipolygon relations
        String overpassQuery = String.format(
                java.util.Locale.ROOT,
                "[out:json][timeout:%d];(node[\"amenity\"=\"hospital\"](around:%d,%.6f,%.6f);way[\"amenity\"=\"hospital\"](around:%d,%.6f,%.6f););out center tags %d;",
                timeoutSec, radiusMeters, lat, lon, radiusMeters, lat, lon, maxResults * 2
        );

        try {
            String encodedBody = "data=" + java.net.URLEncoder.encode(overpassQuery, java.nio.charset.StandardCharsets.UTF_8);
            ResponseEntity<String> response = restClient.post()
                    .uri(java.net.URI.create(endpoint))
                    .body(encodedBody)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .toEntity(String.class);

            HttpStatusCode statusCode = response.getStatusCode();
            int code = statusCode.value();

            if (code == 429) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned rate limit (HTTP 429)", endpoint);
                return OverpassResponse.rateLimited(endpoint, 429, radiusKm, "Overpass API rate limit exceeded (HTTP 429).");
            }
            if (code == 504) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned gateway timeout (HTTP 504)", endpoint);
                return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass query timed out (HTTP 504).");
            }
            if (statusCode.is5xxServerError()) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned server error (HTTP {})", endpoint, code);
                return OverpassResponse.serverError(endpoint, code, radiusKm, "Overpass server error (HTTP " + code + ").");
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return OverpassResponse.invalid(endpoint, code, radiusKm, "Empty response received from Overpass.");
            }

            // Check for Overpass runtime errors or HTML pages returned with HTTP 200
            if (body.contains("runtime error: Query timed out") || body.contains("Timeout exceeded") || body.contains("timeout reached")) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned runtime timeout in body", endpoint);
                return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass query timed out during execution.");
            }
            if (body.contains("rate_limited") || body.contains("Too Many Requests")) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned rate limit in body", endpoint);
                return OverpassResponse.rateLimited(endpoint, 429, radiusKm, "Overpass rate limit exceeded.");
            }
            if (body.trim().startsWith("<") || body.contains("<!DOCTYPE") || body.contains("<html")) {
                log.warn("[HospitalSearchService] Overpass endpoint [{}] returned HTML instead of JSON: {}", endpoint, getSanitizedSnippet(body));
                if (body.contains("Gateway Timeout") || body.contains("504")) {
                    return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass gateway timeout (HTTP 504).");
                }
                return OverpassResponse.invalid(endpoint, code, radiusKm, "Non-JSON response received from Overpass.");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode elements = root.path("elements");
            if (elements.isMissingNode() || !elements.isArray()) {
                log.warn("[HospitalSearchService] Overpass response missing 'elements' array: {}", getSanitizedSnippet(body));
                return OverpassResponse.invalid(endpoint, code, radiusKm, "Invalid response schema: missing elements array.");
            }
            if (elements.isEmpty()) {
                return OverpassResponse.empty(endpoint, radiusKm);
            }

            return OverpassResponse.success(endpoint, elements, radiusKm);

        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Overpass client error {} from [{}]: {}", code, endpoint, getSanitizedSnippet(ex.getResponseBodyAsString()));
            if (code == 429) {
                return OverpassResponse.rateLimited(endpoint, 429, radiusKm, "Overpass rate limit exceeded (HTTP 429).");
            }
            return OverpassResponse.clientError(endpoint, code, radiusKm, "Overpass client error (HTTP " + code + ").");
        } catch (HttpServerErrorException ex) {
            int code = ex.getStatusCode().value();
            log.warn("[HospitalSearchService] Overpass server error {} from [{}]: {}", code, endpoint, getSanitizedSnippet(ex.getResponseBodyAsString()));
            if (code == 504) {
                return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass gateway timeout (HTTP 504).");
            }
            return OverpassResponse.serverError(endpoint, code, radiusKm, "Overpass server error (HTTP " + code + ").");
        } catch (ResourceAccessException ex) {
            log.warn("[HospitalSearchService] Overpass request to [{}] timed out / network error: {}", endpoint, ex.getMessage());
            return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass request timed out or network connection failed.");
        } catch (JsonProcessingException ex) {
            log.warn("[HospitalSearchService] Failed to parse Overpass JSON from [{}]: {}", endpoint, ex.getMessage());
            return OverpassResponse.invalid(endpoint, 200, radiusKm, "Invalid JSON received from Overpass.");
        } catch (Exception ex) {
            if (isTimeoutOrConnectionError(ex)) {
                log.warn("[HospitalSearchService] Overpass request to [{}] connection/timeout error: {}", endpoint, ex.getMessage());
                return OverpassResponse.timedOut(endpoint, 504, radiusKm, "Overpass connection or read timed out.");
            }
            log.warn("[HospitalSearchService] Unexpected error querying Overpass [{}]: {}", endpoint, ex.getMessage());
            return OverpassResponse.serverError(endpoint, 500, radiusKm, "Overpass service error: " + ex.getClass().getSimpleName());
        }
    }

    private boolean isTimeoutOrConnectionError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException ||
                current instanceof java.net.ConnectException ||
                current instanceof ResourceAccessException ||
                current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("timed out") || lower.contains("timeout") ||
                    lower.contains("connection refused") || lower.contains("connect timed out") ||
                    lower.contains("read timed out") || lower.contains("connection reset")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String getSanitizedSnippet(String text) {
        if (text == null) return "";
        String singleLine = text.replaceAll("[\\r\\n]+", " ").trim();
        if (singleLine.length() > 200) {
            return singleLine.substring(0, 200) + "...";
        }
        return singleLine;
    }

    public static class OverpassResponse {
        public enum Status {
            SUCCESS,
            EMPTY,
            RATE_LIMITED,
            TIMED_OUT,
            SERVER_ERROR,
            CLIENT_ERROR,
            INVALID_RESPONSE
        }

        private final Status status;
        private final int statusCode;
        private final JsonNode elements;
        private final double radiusUsedKm;
        private final String endpointUsed;
        private final String message;

        public OverpassResponse(Status status, int statusCode, JsonNode elements, double radiusUsedKm, String endpointUsed, String message) {
            this.status = status;
            this.statusCode = statusCode;
            this.elements = elements;
            this.radiusUsedKm = radiusUsedKm;
            this.endpointUsed = endpointUsed;
            this.message = message;
        }

        public static OverpassResponse success(String endpoint, JsonNode elements, double radiusUsedKm) {
            return new OverpassResponse(Status.SUCCESS, 200, elements, radiusUsedKm, endpoint, "Success");
        }

        public static OverpassResponse empty(String endpoint, double radiusUsedKm) {
            return new OverpassResponse(Status.EMPTY, 200, null, radiusUsedKm, endpoint, "No elements found");
        }

        public static OverpassResponse rateLimited(String endpoint, int code, double radiusUsedKm, String msg) {
            return new OverpassResponse(Status.RATE_LIMITED, code, null, radiusUsedKm, endpoint, msg);
        }

        public static OverpassResponse timedOut(String endpoint, int code, double radiusUsedKm, String msg) {
            return new OverpassResponse(Status.TIMED_OUT, code, null, radiusUsedKm, endpoint, msg);
        }

        public static OverpassResponse serverError(String endpoint, int code, double radiusUsedKm, String msg) {
            return new OverpassResponse(Status.SERVER_ERROR, code, null, radiusUsedKm, endpoint, msg);
        }

        public static OverpassResponse clientError(String endpoint, int code, double radiusUsedKm, String msg) {
            return new OverpassResponse(Status.CLIENT_ERROR, code, null, radiusUsedKm, endpoint, msg);
        }

        public static OverpassResponse invalid(String endpoint, int code, double radiusUsedKm, String msg) {
            return new OverpassResponse(Status.INVALID_RESPONSE, code, null, radiusUsedKm, endpoint, msg);
        }

        public Status getStatus() { return status; }
        public int getStatusCode() { return statusCode; }
        public JsonNode getElements() { return elements; }
        public double getRadiusUsedKm() { return radiusUsedKm; }
        public String getEndpointUsed() { return endpointUsed; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
        public boolean isEmpty() { return status == Status.EMPTY; }
    }

    public static class SettlementCandidate {
        public final double lat;
        public final double lon;
        public final String displayName;
        public final String district;
        public final String state;
        public final String postcode;
        public final boolean isSettlement;
        public final boolean isPostalArea;

        public SettlementCandidate(double lat, double lon, String displayName, String district, String state, String postcode, boolean isSettlement, boolean isPostalArea) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
            this.district = district;
            this.state = state;
            this.postcode = postcode;
            this.isSettlement = isSettlement;
            this.isPostalArea = isPostalArea;
        }

        public SettlementCandidate(double lat, double lon, String displayName, String district, String state, String postcode, boolean isSettlement) {
            this(lat, lon, displayName, district, state, postcode, isSettlement, false);
        }
    }

    public static class SettlementResolution {
        private final SettlementCandidate selected;
        private final boolean ambiguous;
        private final String message;

        public SettlementResolution(SettlementCandidate selected, boolean ambiguous, String message) {
            this.selected = selected;
            this.ambiguous = ambiguous;
            this.message = message;
        }

        public static SettlementResolution selected(SettlementCandidate candidate) {
            return new SettlementResolution(candidate, false, null);
        }

        public static SettlementResolution ambiguous(String message) {
            return new SettlementResolution(null, true, message);
        }

        public static SettlementResolution notFound(String message) {
            return new SettlementResolution(null, false, message);
        }

        public SettlementCandidate getSelected() { return selected; }
        public boolean isAmbiguous() { return ambiguous; }
        public String getMessage() { return message; }
    }

    public static class GeocodingException extends RuntimeException {
        private final int statusCode;
        private final boolean rateLimited;
        private final boolean timedOut;

        public GeocodingException(String message, int statusCode, boolean rateLimited, boolean timedOut, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
            this.rateLimited = rateLimited;
            this.timedOut = timedOut;
        }

        public int getStatusCode() { return statusCode; }
        public boolean isRateLimited() { return rateLimited; }
        public boolean isTimedOut() { return timedOut; }
    }

    public double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static class TargetLocationResult {
        private final SettlementCandidate candidate;
        private final String resolvedLocationName;
        private final HospitalSearchResult notFoundResult;

        public TargetLocationResult(SettlementCandidate candidate, String resolvedLocationName) {
            this.candidate = candidate;
            this.resolvedLocationName = resolvedLocationName;
            this.notFoundResult = null;
        }

        public TargetLocationResult(HospitalSearchResult notFoundResult) {
            this.candidate = null;
            this.resolvedLocationName = null;
            this.notFoundResult = notFoundResult;
        }

        public boolean isSuccess() {
            return candidate != null;
        }

        public double getLat() {
            return candidate != null ? candidate.lat : 0.0;
        }

        public double getLon() {
            return candidate != null ? candidate.lon : 0.0;
        }

        public SettlementCandidate getCandidate() {
            return candidate;
        }

        public String getResolvedLocationName() {
            return resolvedLocationName;
        }

        public HospitalSearchResult getNotFoundResult() {
            return notFoundResult;
        }
    }

    public static class GeoapifyResponse {
        public enum Status {
            SUCCESS,
            EMPTY,
            RATE_LIMITED,
            TIMED_OUT,
            SERVER_ERROR,
            CLIENT_ERROR,
            INVALID_RESPONSE
        }

        private final Status status;
        private final int statusCode;
        private final List<HospitalSearchResult.HospitalItem> hospitals;
        private final double radiusUsedKm;
        private final String message;

        public GeoapifyResponse(Status status, int statusCode, List<HospitalSearchResult.HospitalItem> hospitals, double radiusUsedKm, String message) {
            this.status = status;
            this.statusCode = statusCode;
            this.hospitals = hospitals != null ? hospitals : List.of();
            this.radiusUsedKm = radiusUsedKm;
            this.message = message;
        }

        public static GeoapifyResponse success(List<HospitalSearchResult.HospitalItem> hospitals, double radiusUsedKm) {
            return new GeoapifyResponse(Status.SUCCESS, 200, hospitals, radiusUsedKm, "Success");
        }

        public static GeoapifyResponse empty(double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.EMPTY, 200, List.of(), radiusUsedKm, message);
        }

        public static GeoapifyResponse rateLimited(int statusCode, double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.RATE_LIMITED, statusCode, List.of(), radiusUsedKm, message);
        }

        public static GeoapifyResponse timedOut(int statusCode, double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.TIMED_OUT, statusCode, List.of(), radiusUsedKm, message);
        }

        public static GeoapifyResponse serverError(int statusCode, double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.SERVER_ERROR, statusCode, List.of(), radiusUsedKm, message);
        }

        public static GeoapifyResponse clientError(int statusCode, double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.CLIENT_ERROR, statusCode, List.of(), radiusUsedKm, message);
        }

        public static GeoapifyResponse invalid(double radiusUsedKm, String message) {
            return new GeoapifyResponse(Status.INVALID_RESPONSE, 400, List.of(), radiusUsedKm, message);
        }

        public Status getStatus() { return status; }
        public int getStatusCode() { return statusCode; }
        public List<HospitalSearchResult.HospitalItem> getHospitals() { return hospitals; }
        public double getRadiusUsedKm() { return radiusUsedKm; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
    }
}

