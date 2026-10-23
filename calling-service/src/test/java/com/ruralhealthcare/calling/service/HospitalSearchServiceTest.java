package com.ruralhealthcare.calling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.dto.HospitalSearchResult;
import com.ruralhealthcare.calling.service.hospital.HospitalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalSearchServiceTest {

    private HospitalSearchService searchService;

    @BeforeEach
    void setUp() {
        CallingProperties props = new CallingProperties();
        props.getHospitalSearch().setProvider("mock");
        searchService = new HospitalSearchService(props, RestClient.builder(), new ObjectMapper());
    }

    @Test
    @DisplayName("Mock search returns verified hospitals for recognized location")
    void testSearchHospitalsFound() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertFalse(result.getHospitals().isEmpty());
        assertTrue(result.getHospitals().get(0).getName().contains("Hospital"));
    }

    @Test
    @DisplayName("Search returns honest not-found explanation when location has no verified hospitals")
    void testSearchHospitalsNotFound() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("nowhere-rural-desert");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("No verified hospitals found"));
        assertTrue(result.getHospitals().isEmpty());
    }

    @Test
    @DisplayName("Empty or null location query returns clear prompt for location")
    void testSearchHospitalsEmptyQuery() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("No location was provided"));
    }

    @Test
    @DisplayName("Ambiguous location without district (Alandi) prompts for clarifying district or PIN")
    void testSearchAlandiAmbiguousAsksClarification() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Maharashtra");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("Ambiguous location"));
        assertTrue(result.getMessage().contains("Pune district") || result.getMessage().contains("412105"));
        assertTrue(result.getMessage().contains("Nanded district") || result.getMessage().contains("431736"));
    }

    @Test
    @DisplayName("Explicit location (Alandi, Pune) stays in Alandi Pune and returns verified local hospitals")
    void testSearchAlandiPuneResolvesExplicitLocation() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertFalse(result.getHospitals().isEmpty());
        assertTrue(result.getQueryLocation().contains("Alandi"));
        assertTrue(result.getQueryLocation().contains("Pune"));
        // Ensure no Ahmednagar results
        assertFalse(result.getQueryLocation().contains("Ahmednagar"));
        for (HospitalSearchResult.HospitalItem h : result.getHospitals()) {
            assertTrue(h.getAddress().contains("Alandi, Pune"));
            assertFalse(h.getAddress().contains("Ahmednagar"));
        }
    }

    @Test
    @DisplayName("Explicit PIN code (Alandi 412105) resolves to Alandi Pune")
    void testSearchAlandiPinCodeResolvesExplicitLocation() {
        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi 412105");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertTrue(result.getQueryLocation().contains("Alandi"));
        assertTrue(result.getQueryLocation().contains("Pune"));
    }

    @Test
    @DisplayName("Haversine distance calculation produces correct geographic distance")
    void testHaversineDistance() {
        // Alandi (18.677, 73.898) to Pune Shivajinagar (18.531, 73.844) ~ 17.2 km
        double distance = searchService.calculateHaversineKm(18.677, 73.898, 18.531, 73.844);
        assertTrue(distance > 16.0 && distance < 19.0, "Expected distance around 17km, got: " + distance);
    }

    @Test
    @DisplayName("Overpass parser sorts nearest first and filters out hospitals outside radius")
    void testParseOverpassElements_SortingAndRadiusFilter() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "type": "node",
                    "lat": 18.6850,
                    "lon": 73.9000,
                    "tags": {"name": "Far Hospital", "amenity": "hospital"}
                },
                {
                    "type": "node",
                    "lat": 18.6775,
                    "lon": 73.8985,
                    "tags": {"name": "Nearest Clinic", "amenity": "hospital"}
                },
                {
                    "type": "node",
                    "lat": 19.5000,
                    "lon": 74.5000,
                    "tags": {"name": "Too Far Away Hospital (100km)", "amenity": "hospital"}
                }
            ]
            """;
        var elements = mapper.readTree(json);
        var hospitals = searchService.parseOverpassElements(elements, 18.6770, 73.8980, "Alandi, Pune");

        assertNotNull(hospitals);
        // "Too Far Away Hospital" (approx 100km away) must be excluded by 25km radius
        org.junit.jupiter.api.Assertions.assertEquals(2, hospitals.size());
        // Nearest Clinic must be first
        org.junit.jupiter.api.Assertions.assertEquals("Nearest Clinic", hospitals.get(0).getName());
        org.junit.jupiter.api.Assertions.assertEquals("Far Hospital", hospitals.get(1).getName());
        assertTrue(hospitals.get(0).getDistanceKm() < hospitals.get(1).getDistanceKm());
    }

    @Test
    @DisplayName("Overpass parser excludes anonymous records and records with invalid coordinates")
    void testParseOverpassElements_ExcludesAnonymousAndInvalid() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "type": "node",
                    "lat": 18.6775,
                    "lon": 73.8985,
                    "tags": {"amenity": "hospital"}
                },
                {
                    "type": "node",
                    "lat": 0.0,
                    "lon": 0.0,
                    "tags": {"name": "Null Island Hospital", "amenity": "hospital"}
                },
                {
                    "type": "node",
                    "tags": {"name": "Missing Coords Hospital", "amenity": "hospital"}
                },
                {
                    "type": "node",
                    "lat": 18.6775,
                    "lon": 73.8985,
                    "tags": {"name": "Valid Community Hospital", "amenity": "hospital"}
                }
            ]
            """;
        var elements = mapper.readTree(json);
        var hospitals = searchService.parseOverpassElements(elements, 18.6770, 73.8980, "Alandi, Pune");

        org.junit.jupiter.api.Assertions.assertEquals(1, hospitals.size());
        org.junit.jupiter.api.Assertions.assertEquals("Valid Community Hospital", hospitals.get(0).getName());
    }

    @Test
    @DisplayName("Overpass parser deduplicates duplicate hospital entries")
    void testParseOverpassElements_Deduplicates() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "type": "node",
                    "lat": 18.6775,
                    "lon": 73.8985,
                    "tags": {"name": "City Care Hospital", "amenity": "hospital"}
                },
                {
                    "type": "way",
                    "center": {"lat": 18.6776, "lon": 73.8986},
                    "tags": {"name": "City Care Hospital", "amenity": "hospital"}
                }
            ]
            """;
        var elements = mapper.readTree(json);
        var hospitals = searchService.parseOverpassElements(elements, 18.6770, 73.8980, "Alandi, Pune");

        // The duplicate entry for "City Care Hospital" must be removed
        org.junit.jupiter.api.Assertions.assertEquals(1, hospitals.size());
        org.junit.jupiter.api.Assertions.assertEquals("City Care Hospital", hospitals.get(0).getName());
    }

    @Test
    @DisplayName("Settlement resolution skips waterways (Alandi River) and prioritizes inhabited places")
    void testResolveSettlement_SkipsWaterway() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "class": "waterway",
                    "type": "river",
                    "lat": 20.068,
                    "lon": 73.697,
                    "display_name": "Alandi River, Gangapur, Nashik",
                    "address": {"county": "Nashik", "state": "Maharashtra"}
                },
                {
                    "class": "place",
                    "type": "village",
                    "lat": 18.677,
                    "lon": 73.898,
                    "display_name": "Alandi, Khed, Pune",
                    "address": {"county": "Pune", "state": "Maharashtra", "postcode": "412105"}
                }
            ]
            """;
        var candidates = mapper.readTree(json);
        var resolution = searchService.resolveSettlement(candidates, "Alandi");

        assertNotNull(resolution.getSelected());
        assertFalse(resolution.isAmbiguous());
        // Must select Alandi village in Pune (lat 18.677), NOT the river at lat 20.068
        org.junit.jupiter.api.Assertions.assertEquals(18.677, resolution.getSelected().lat);
        org.junit.jupiter.api.Assertions.assertEquals("Pune", resolution.getSelected().district);
    }

    @Test
    @DisplayName("Settlement resolution rejects silent selection across multiple districts and demands clarification")
    void testResolveSettlement_DemandsClarificationAcrossDistricts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "class": "place",
                    "type": "village",
                    "lat": 18.677,
                    "lon": 73.898,
                    "display_name": "Alandi, Pune, Maharashtra",
                    "address": {"county": "Pune", "state": "Maharashtra", "postcode": "412105"}
                },
                {
                    "class": "place",
                    "type": "village",
                    "lat": 19.123,
                    "lon": 77.456,
                    "display_name": "Alandi, Nanded, Maharashtra",
                    "address": {"county": "Nanded", "state": "Maharashtra", "postcode": "431736"}
                }
            ]
            """;
        var candidates = mapper.readTree(json);
        var resolution = searchService.resolveSettlement(candidates, "Alandi");

        assertTrue(resolution.isAmbiguous(), "Must be flagged as ambiguous when matching multiple districts");
        org.junit.jupiter.api.Assertions.assertNull(resolution.getSelected());
        assertTrue(resolution.getMessage().contains("Pune"));
        assertTrue(resolution.getMessage().contains("Nanded"));
    }

    @Test
    @DisplayName("buildGeocodingQueries generates sensible fallback queries for Alandi Pune with PIN 412105")
    void testBuildGeocodingQueries_GeneratesSensibleFallbacks() {
        java.util.List<String> queries = searchService.buildGeocodingQueries("Alandi, Pune, Maharashtra, 412105");

        assertNotNull(queries);
        assertTrue(queries.contains("Alandi, Pune, Maharashtra, 412105"), "Must include exact raw query");
        assertTrue(queries.contains("Alandi, Pune, Maharashtra, India"), "Must include normalized settlement query with country");
        assertTrue(queries.contains("Alandi, Khed, Pune, Maharashtra, India"), "Must include Khed taluka query");
        assertTrue(queries.contains("412105, Maharashtra, India"), "Must include PIN code fallback query");
        assertTrue(queries.contains("412105, India"), "Must include 412105, India");
        // Verify order: raw query is first
        org.junit.jupiter.api.Assertions.assertEquals("Alandi, Pune, Maharashtra, 412105", queries.get(0));
    }

    @Test
    @DisplayName("Settlement resolution rejects candidates from incorrect district when intended district is specified")
    void testResolveSettlement_RejectsIncorrectDistrict() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "class": "place",
                    "type": "village",
                    "lat": 19.123,
                    "lon": 77.456,
                    "display_name": "Alandi, Nanded, Maharashtra",
                    "address": {"county": "Nanded", "state_district": "Nanded", "state": "Maharashtra", "postcode": "431736"}
                },
                {
                    "class": "place",
                    "type": "village",
                    "lat": 18.677,
                    "lon": 73.898,
                    "display_name": "Alandi, Khed, Pune",
                    "address": {"county": "Khed", "state_district": "Pune", "state": "Maharashtra", "postcode": "412105"}
                }
            ]
            """;
        var candidates = mapper.readTree(json);
        // Explicitly specifying "Pune" as intended district
        var resolution = searchService.resolveSettlement(candidates, "Alandi, Pune", "Pune");

        assertNotNull(resolution.getSelected());
        // Candidate in Nanded must be rejected, Pune candidate must be selected
        org.junit.jupiter.api.Assertions.assertEquals("Pune", resolution.getSelected().district);
        org.junit.jupiter.api.Assertions.assertEquals(18.677, resolution.getSelected().lat);
    }

    @Test
    @DisplayName("Settlement resolution returns not found when all candidates are from incorrect district")
    void testResolveSettlement_AllCandidatesWrongDistrict() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            [
                {
                    "class": "place",
                    "type": "village",
                    "lat": 19.123,
                    "lon": 77.456,
                    "display_name": "Alandi, Nanded, Maharashtra",
                    "address": {"county": "Nanded", "state_district": "Nanded", "state": "Maharashtra", "postcode": "431736"}
                }
            ]
            """;
        var candidates = mapper.readTree(json);
        var resolution = searchService.resolveSettlement(candidates, "Alandi, Pune", "Pune");

        org.junit.jupiter.api.Assertions.assertNull(resolution.getSelected());
        assertTrue(resolution.getMessage().contains("No matching locations found in district 'Pune'"));
    }

    @Test
    @DisplayName("Exact failing input 'Alandi, Pune, Maharashtra, 412105' resolves via fallback query and returns verified hospitals")
    void testSearchExactFailingInput_AlandiWithPin_ResolvesViaFallback() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        String nominatimEmptyJson = "[]";
        String nominatimAlandiJson = """
            [
                {
                    "class": "place",
                    "type": "village",
                    "lat": 18.6772,
                    "lon": 73.8981,
                    "display_name": "Alandi, Khed, Pune, Maharashtra, 412105, India",
                    "address": {
                        "village": "Alandi",
                        "county": "Khed",
                        "state_district": "Pune",
                        "state": "Maharashtra",
                        "postcode": "412105"
                    }
                }
            ]
            """;
        String overpassHospitalsJson = """
            {
                "elements": [
                    {
                        "type": "node",
                        "lat": 18.6750,
                        "lon": 73.8960,
                        "tags": {
                            "name": "Jaihind Multi & Super Speciality Hospital",
                            "amenity": "hospital",
                            "addr:street": "Near Markal Road",
                            "addr:city": "Alandi"
                        }
                    },
                    {
                        "type": "node",
                        "lat": 18.6790,
                        "lon": 73.8940,
                        "tags": {
                            "name": "Aavati Hospital",
                            "amenity": "hospital",
                            "addr:street": "Dehu Phata",
                            "addr:city": "Alandi"
                        }
                    }
                ]
            }
            """;

        // 1. Initial raw query "Alandi, Pune, Maharashtra, 412105" returns empty []
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.both(org.hamcrest.Matchers.containsString("nominatim"))
                        .and(org.hamcrest.Matchers.containsString("412105"))))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        nominatimEmptyJson, org.springframework.http.MediaType.APPLICATION_JSON));

        // 2. Fallback query "Alandi, Pune, Maharashtra, India" returns valid Alandi settlement in Pune
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.both(org.hamcrest.Matchers.containsString("nominatim"))
                        .and(org.hamcrest.Matchers.containsString("India"))))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        nominatimAlandiJson, org.springframework.http.MediaType.APPLICATION_JSON));

        // 3. Overpass API returns hospitals
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("interpreter")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        overpassHospitalsJson, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune, Maharashtra, 412105");

        assertNotNull(result);
        assertTrue(result.isFound(), "Hospital search must succeed for Alandi, Pune, Maharashtra, 412105 via fallback");
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getHospitals().size());
        org.junit.jupiter.api.Assertions.assertEquals("Jaihind Multi & Super Speciality Hospital", result.getHospitals().get(0).getName());
        org.junit.jupiter.api.Assertions.assertEquals("Aavati Hospital", result.getHospitals().get(1).getName());
        assertTrue(result.getHospitals().get(0).getDistanceKm() <= result.getHospitals().get(1).getDistanceKm());
        mockServer.verify();
    }

    @Test
    @DisplayName("Postal area is used as supporting location evidence when settlement cannot be resolved directly")
    void testSearch_PostalAreaUsedAsSupportingEvidence() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        String nominatimPostalCodeJson = """
            [
                {
                    "class": "place",
                    "type": "postcode",
                    "lat": 18.6593,
                    "lon": 73.8854,
                    "display_name": "412105, Haveli, Pune, Maharashtra, India",
                    "address": {
                        "county": "Haveli",
                        "state_district": "Pune",
                        "state": "Maharashtra",
                        "postcode": "412105"
                    }
                }
            ]
            """;
        String overpassHospitalsJson = """
            {
                "elements": [
                    {
                        "type": "node",
                        "lat": 18.6600,
                        "lon": 73.8860,
                        "tags": {
                            "name": "Area Primary Health Centre",
                            "amenity": "hospital"
                        }
                    }
                ]
            }
            """;

        // Any settlement query returns empty, but PIN query returns postcode
        mockServer.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(),
                org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                        org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(request -> {
                    String uri = request.getURI().toString();
                    if (uri.contains("412105")) {
                        return org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                                nominatimPostalCodeJson, org.springframework.http.MediaType.APPLICATION_JSON).createResponse(request);
                    }
                    return org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                            "[]", org.springframework.http.MediaType.APPLICATION_JSON).createResponse(request);
                });

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("interpreter")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        overpassHospitalsJson, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("412105, Maharashtra, India");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertTrue(result.getQueryLocation().contains("Postal Area PIN 412105"));
        org.junit.jupiter.api.Assertions.assertEquals(1, result.getHospitals().size());
        org.junit.jupiter.api.Assertions.assertEquals("Area Primary Health Centre", result.getHospitals().get(0).getName());
    }

    @Test
    @DisplayName("External service failure - Rate limit (HTTP 429) returns informative error message")
    void testExternalServiceFailure_RateLimit429() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(429)
                        .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                        .body("Bandwidth limit exceeded"));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("rate limit exceeded (HTTP 429)"), "Must explain rate limit exceeded");
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Nominatim", result.getProvider());
    }

    @Test
    @DisplayName("External service failure - Server error (HTTP 500) returns informative error message")
    void testExternalServiceFailure_ServerError500() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("temporarily unavailable (HTTP 500)"));
    }

    @Test
    @DisplayName("No results across all fallback queries returns honest coordinates verification message")
    void testNoResultResponses_AcrossAllFallbacks() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(),
                org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                        org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "[]", org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("NonExistentUnknownPlace123");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("Could not verify geographic coordinates"));
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Nominatim", result.getProvider());
    }

    private static final String MOCK_NOMINATIM_ALANDI_NODE = """
            [
                {
                    "class": "place",
                    "type": "village",
                    "lat": 18.6772,
                    "lon": 73.8981,
                    "display_name": "Alandi, Khed, Pune, Maharashtra, 412105, India",
                    "address": {
                        "village": "Alandi",
                        "county": "Khed",
                        "state_district": "Pune",
                        "state": "Maharashtra",
                        "postcode": "412105"
                    }
                }
            ]
            """;

    private static final String MOCK_OVERPASS_HOSPITALS_NODE = """
            {
                "elements": [
                    {
                        "type": "node",
                        "lat": 18.6750,
                        "lon": 73.8960,
                        "tags": {
                            "name": "Jaihind Multi & Super Speciality Hospital",
                            "amenity": "hospital",
                            "addr:street": "Near Markal Road",
                            "addr:city": "Alandi"
                        }
                    },
                    {
                        "type": "node",
                        "lat": 18.6790,
                        "lon": 73.8940,
                        "tags": {
                            "name": "Aavati Hospital",
                            "amenity": "hospital",
                            "addr:street": "Dehu Phata",
                            "addr:city": "Alandi"
                        }
                    }
                ]
            }
            """;

    @Test
    @DisplayName("Overpass mock: Successful retrieval of genuine hospitals within primary radius")
    void testOverpassSearch_SuccessfulRetrieval() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_OVERPASS_HOSPITALS_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getHospitals().size());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Registry", result.getProvider());
        assertTrue(result.getMessage().contains("Found 2 verified healthcare facility(ies) within 25 km"));
        org.junit.jupiter.api.Assertions.assertEquals(25.0, result.getSearchRadiusKm());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Broad search times out on primary, retry with smaller radius succeeds")
    void testOverpassSearch_PrimaryTimeout_SmallerRadiusSuccess() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // 1. Broad search (25km) on primary times out (HTTP 504)
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(504)
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body("<html><body>Gateway Timeout</body></html>"));

        // 2. Retry with smaller fallback radius (10km) on primary succeeds
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_OVERPASS_HOSPITALS_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getHospitals().size());
        assertTrue(result.getMessage().contains("Found 2 verified healthcare facility(ies) within 10 km"),
                "Result message must report fallback radius of 10 km: " + result.getMessage());
        org.junit.jupiter.api.Assertions.assertEquals(10.0, result.getSearchRadiusKm());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Primary endpoint timeout followed by fallback endpoint success")
    void testOverpassSearch_PrimaryTimeout_FallbackEndpointSuccess() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // 1. Broad query (25km) on primary endpoint times out (HTTP 504)
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(504)
                        .body("Gateway Timeout"));

        // 2. Retry query (10km) on primary endpoint also times out
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(504)
                        .body("Gateway Timeout"));

        // 3. Fallback endpoint (lz4) succeeds with hospital elements
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_OVERPASS_HOSPITALS_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getHospitals().size());
        org.junit.jupiter.api.Assertions.assertEquals(10.0, result.getSearchRadiusKm());
        assertTrue(result.getMessage().contains("Found 2 verified healthcare facility(ies) within 10 km"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Primary endpoint HTTP 500 followed by fallback endpoint success")
    void testOverpassSearch_PrimaryHttp500_FallbackSuccess() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // 1. Primary returns HTTP 500 Internal Server Error
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        // 2. Fallback endpoint (lz4) succeeds with genuine hospital elements
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_OVERPASS_HOSPITALS_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound(), "Search must succeed when fallback succeeds after primary HTTP 500");
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getHospitals().size());
        org.junit.jupiter.api.Assertions.assertEquals(10.0, result.getSearchRadiusKm());
        assertTrue(result.getMessage().contains("Found 2 verified healthcare facility(ies) within 10 km"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: HTTP 429 Rate Limit returns informative message without overloading")
    void testOverpassSearch_Http429_RateLimited() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // Primary endpoint returns 429
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(429)
                        .body("Rate limit exceeded"));

        // Fallback endpoint also returns 429
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(429)
                        .body("Rate limit exceeded"));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Overpass", result.getProvider());
        assertTrue(result.getMessage().contains("rate limit exceeded (HTTP 429)"), "Must explain rate limit exceeded: " + result.getMessage());
        assertTrue(result.getHospitals().isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: HTTP 5xx Server Error returns informative message")
    void testOverpassSearch_Http5xx_ServerError() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // Primary returns 500
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        // Fallback returns 503
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(503)
                        .body("Service Unavailable"));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Overpass", result.getProvider());
        assertTrue(result.getMessage().contains("temporarily unavailable (HTTP 503)"), "Must explain server error: " + result.getMessage());
        assertTrue(result.getHospitals().isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Both endpoints timing out returns honest timeout explanation without fabricating data")
    void testOverpassSearch_BothEndpointsTimingOut() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // Primary 25km times out
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(504)
                        .body("Gateway Timeout"));

        // Primary 10km retry times out
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(
                        new java.net.SocketTimeoutException("Read timed out")));

        // Fallback endpoint times out
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus(504)
                        .body("Gateway Timeout"));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Overpass", result.getProvider());
        assertTrue(result.getMessage().contains("temporarily timed out"), "Must explain timeout honestly: " + result.getMessage());
        assertTrue(result.getHospitals().isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Malformed HTML or invalid JSON response returns honest unavailable message")
    void testOverpassSearch_MalformedResponse() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // Primary returns HTML error page with 200 OK
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "<html><body><h3>Error: bad query</h3></body></html>", org.springframework.http.MediaType.TEXT_HTML));

        // Fallback returns corrupt JSON missing 'elements' array
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("lz4")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"status\": \"corrupt\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Overpass", result.getProvider());
        assertTrue(result.getMessage().contains("currently unavailable"), "Must explain unavailable: " + result.getMessage());
        assertTrue(result.getHospitals().isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Overpass mock: Valid empty results returns honest explanation that no hospitals were recorded")
    void testOverpassSearch_ValidEmptyResults() {
        RestClient.Builder builder = RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        CallingProperties osmProps = new CallingProperties();
        osmProps.getHospitalSearch().setProvider("osm");
        HospitalSearchService osmService = new HospitalSearchService(osmProps, builder, new ObjectMapper());

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("nominatim")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        MOCK_NOMINATIM_ALANDI_NODE, org.springframework.http.MediaType.APPLICATION_JSON));

        // Overpass returns valid JSON with 0 elements
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                org.hamcrest.Matchers.containsString("overpass-api.de")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"elements\": []}", org.springframework.http.MediaType.APPLICATION_JSON));

        HospitalSearchResult result = osmService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        org.junit.jupiter.api.Assertions.assertEquals("OpenStreetMap Overpass", result.getProvider());
        assertTrue(result.getMessage().contains("No verified hospitals recorded within 25 km"),
                "Must report honest empty explanation with radius: " + result.getMessage());
        assertTrue(result.getHospitals().isEmpty());
        mockServer.verify();
    }
}
