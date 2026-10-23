package com.ruralhealthcare.calling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.dto.HospitalSearchResult;
import com.ruralhealthcare.calling.service.hospital.HospitalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeoapifyHospitalSearchTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private CallingProperties properties;
    private ObjectMapper objectMapper;
    private HospitalSearchService searchService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        properties = new CallingProperties();
        properties.getHospitalSearch().setProvider("geoapify");
        properties.getHospitalSearch().getGeoapify().setApiKey("test_geoapify_key_12345");
        properties.getHospitalSearch().getGeoapify().setInitialRadiusKm(10.0);
        properties.getHospitalSearch().getGeoapify().setExpandedRadiusKm(25.0);
        properties.getHospitalSearch().getGeoapify().setMinResultsToExpand(2);
        properties.getHospitalSearch().getGeoapify().setLimit(5);
        properties.getHospitalSearch().setMaxResults(5);

        objectMapper = new ObjectMapper();
        searchService = new HospitalSearchService(properties, restClientBuilder, objectMapper);
    }

    private static final String MOCK_NOMINATIM_ALANDI = """
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

    private static final String MOCK_GEOAPIFY_SUCCESS_3_HOSPITALS = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.8960, 18.6750]
                  },
                  "properties": {
                    "name": "Jaihind Multi & Super Speciality Hospital",
                    "formatted": "Near Markal Road, Alandi, Pune, Maharashtra 412105, India",
                    "street": "Markal Road",
                    "city": "Alandi",
                    "district": "Pune",
                    "lat": 18.6750,
                    "lon": 73.8960,
                    "distance": 320,
                    "categories": ["healthcare", "healthcare.hospital"],
                    "datasource": {
                      "raw": {
                        "phone": "020-27185000",
                        "emergency:phone": "108",
                        "amenity": "hospital"
                      }
                    }
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.8940, 18.6790]
                  },
                  "properties": {
                    "name": "Aavati Healthcare Hospital",
                    "formatted": "Dehu Phata, Alandi, Pune, Maharashtra, India",
                    "street": "Dehu Phata",
                    "city": "Alandi",
                    "district": "Pune",
                    "lat": 18.6790,
                    "lon": 73.8940,
                    "distance": 480,
                    "categories": ["healthcare", "healthcare.hospital"]
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.9020, 18.6710]
                  },
                  "properties": {
                    "name": "Rural Hospital Alandi Devachi",
                    "formatted": "Wadmukhwadi Road, Alandi, Pune, Maharashtra, India",
                    "city": "Alandi",
                    "district": "Pune",
                    "lat": 18.6710,
                    "lon": 73.9020,
                    "distance": 810,
                    "categories": ["healthcare", "healthcare.hospital"],
                    "datasource": {
                      "raw": {
                        "healthcare": "Rural Hospital"
                      }
                    }
                  }
                }
              ]
            }
            """;

    private static final String MOCK_GEOAPIFY_ONE_HOSPITAL = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.8960, 18.6750]
                  },
                  "properties": {
                    "name": "Jaihind Multi Speciality Hospital",
                    "formatted": "Near Markal Road, Alandi, Pune, Maharashtra, India",
                    "city": "Alandi",
                    "district": "Pune",
                    "lat": 18.6750,
                    "lon": 73.8960,
                    "distance": 320,
                    "categories": ["healthcare", "healthcare.hospital"]
                  }
                }
              ]
            }
            """;

    private static final String MOCK_GEOAPIFY_EXPANDED_HOSPITALS = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.8960, 18.6750]
                  },
                  "properties": {
                    "name": "Jaihind Multi Speciality Hospital",
                    "formatted": "Near Markal Road, Alandi, Pune, Maharashtra, India",
                    "city": "Alandi",
                    "district": "Pune",
                    "lat": 18.6750,
                    "lon": 73.8960,
                    "distance": 320,
                    "categories": ["healthcare", "healthcare.hospital"]
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [73.8210, 18.6250]
                  },
                  "properties": {
                    "name": "Dr. D.Y. Patil Medical College Hospital",
                    "formatted": "Sant Tukaram Nagar, Pimpri, Pune, Maharashtra 411018, India",
                    "city": "Pimpri",
                    "district": "Pune",
                    "lat": 18.6250,
                    "lon": 73.8210,
                    "distance": 9800,
                    "categories": ["healthcare", "healthcare.hospital"]
                  }
                }
              ]
            }
            """;

    private static final String MOCK_OVERPASS_BACKUP_HOSPITALS = """
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
    @DisplayName("1. Successful Geoapify search: retrieves genuine hospitals sorted nearest first")
    void testGeoapifySearch_SuccessfulNearbyResults() {
        // 1. Nominatim resolves location
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // 2. Geoapify Places API returns 3 facilities in initial 10km radius
        mockServer.expect(requestTo(containsString("api.geoapify.com/v2/places")))
                .andRespond(withSuccess(MOCK_GEOAPIFY_SUCCESS_3_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("Geoapify Places", result.getProvider());
        assertEquals(3, result.getHospitals().size());
        assertEquals(10.0, result.getSearchRadiusKm());

        // Check nearest hospital first
        HospitalSearchResult.HospitalItem first = result.getHospitals().get(0);
        assertEquals("Jaihind Multi & Super Speciality Hospital", first.getName());
        assertEquals(18.6750, first.getLatitude());
        assertEquals(73.8960, first.getLongitude());
        assertNotNull(first.getDistanceKm());
        assertTrue(first.getDistanceKm() <= 10.0);
        assertEquals("020-27185000", first.getContactNumber());
        assertEquals("108", first.getEmergencyNumber());

        // Verify sorted by distance ascending
        for (int i = 0; i < result.getHospitals().size() - 1; i++) {
            assertTrue(result.getHospitals().get(i).getDistanceKm() <= result.getHospitals().get(i + 1).getDistanceKm());
        }

        mockServer.verify();
    }

    @Test
    @DisplayName("2. Radius expansion: expands from 10km to 25km when fewer than 2 results found initially")
    void testGeoapifySearch_RadiusExpansionWhenFewResultsFound() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Initial 10km query returns only 1 hospital
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,10000")))
                .andRespond(withSuccess(MOCK_GEOAPIFY_ONE_HOSPITAL, MediaType.APPLICATION_JSON));

        // Expanded 25km query automatically triggered
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,25000")))
                .andRespond(withSuccess(MOCK_GEOAPIFY_EXPANDED_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("Geoapify Places", result.getProvider());
        assertEquals(2, result.getHospitals().size());
        assertEquals(25.0, result.getSearchRadiusKm());
        assertEquals("Jaihind Multi Speciality Hospital", result.getHospitals().get(0).getName());
        assertEquals("Dr. D.Y. Patil Medical College Hospital", result.getHospitals().get(1).getName());

        mockServer.verify();
    }

    @Test
    @DisplayName("3. Empty results: Geoapify returns empty, seamlessly falls back to OpenStreetMap Overpass")
    void testGeoapifySearch_EmptyResults_FallsBackToOverpass() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Geoapify 10km returns empty
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,10000")))
                .andRespond(withSuccess("{\"type\": \"FeatureCollection\", \"features\": []}", MediaType.APPLICATION_JSON));

        // Geoapify 25km expansion returns empty
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,25000")))
                .andRespond(withSuccess("{\"type\": \"FeatureCollection\", \"features\": []}", MediaType.APPLICATION_JSON));

        // Overpass fallback succeeds
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess(MOCK_OVERPASS_BACKUP_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("OpenStreetMap Registry", result.getProvider(), "Must fallback to OpenStreetMap when Geoapify has 0 results");
        assertEquals(2, result.getHospitals().size());

        mockServer.verify();
    }

    @Test
    @DisplayName("4. Timeout: Geoapify request times out, seamlessly falls back to OpenStreetMap Overpass")
    void testGeoapifySearch_Timeout_FallsBackToOverpass() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Geoapify times out
        mockServer.expect(requestTo(containsString("api.geoapify.com")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        // Overpass fallback succeeds
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess(MOCK_OVERPASS_BACKUP_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("OpenStreetMap Registry", result.getProvider(), "Must fallback to OpenStreetMap on Geoapify timeout");
        assertEquals(2, result.getHospitals().size());

        mockServer.verify();
    }

    @Test
    @DisplayName("5. Rate limit (429): Geoapify rate limit exceeded, seamlessly falls back to OpenStreetMap Overpass")
    void testGeoapifySearch_RateLimit429_FallsBackToOverpass() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Geoapify returns HTTP 429
        mockServer.expect(requestTo(containsString("api.geoapify.com")))
                .andRespond(withRawStatus(429).body("Daily request quota exceeded"));

        // Overpass fallback succeeds
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess(MOCK_OVERPASS_BACKUP_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("OpenStreetMap Registry", result.getProvider(), "Must fallback to OpenStreetMap on Geoapify 429");
        assertEquals(2, result.getHospitals().size());

        mockServer.verify();
    }

    @Test
    @DisplayName("6. Server Error (500): Geoapify returns HTTP 500, seamlessly falls back to OpenStreetMap Overpass")
    void testGeoapifySearch_Http500_FallsBackToOverpass() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Geoapify returns HTTP 500
        mockServer.expect(requestTo(containsString("api.geoapify.com")))
                .andRespond(withServerError());

        // Overpass fallback succeeds
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess(MOCK_OVERPASS_BACKUP_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("OpenStreetMap Registry", result.getProvider(), "Must fallback to OpenStreetMap on Geoapify 500");
        assertEquals(2, result.getHospitals().size());

        mockServer.verify();
    }

    @Test
    @DisplayName("7. Missing API Key: Geoapify API key not configured, seamlessly uses OpenStreetMap Overpass")
    void testGeoapifySearch_MissingApiKey_FallsBackToOverpass() {
        properties.getHospitalSearch().getGeoapify().setApiKey("");

        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Overpass directly called because Geoapify key is empty
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess(MOCK_OVERPASS_BACKUP_HOSPITALS, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertTrue(result.isFound());
        assertEquals("OpenStreetMap Registry", result.getProvider());
        assertEquals(2, result.getHospitals().size());

        mockServer.verify();
    }

    @Test
    @DisplayName("8. Both providers empty: returns honest explanation without inventing hospital data")
    void testGeoapifySearch_BothProvidersEmpty_HonestNotFound() {
        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(MOCK_NOMINATIM_ALANDI, MediaType.APPLICATION_JSON));

        // Geoapify 10km empty
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,10000")))
                .andRespond(withSuccess("{\"type\": \"FeatureCollection\", \"features\": []}", MediaType.APPLICATION_JSON));

        // Geoapify 25km empty
        mockServer.expect(requestTo(containsString("circle:73.898100,18.677200,25000")))
                .andRespond(withSuccess("{\"type\": \"FeatureCollection\", \"features\": []}", MediaType.APPLICATION_JSON));

        // Overpass also returns 0 elements
        mockServer.expect(requestTo(containsString("overpass-api.de")))
                .andRespond(withSuccess("{\"elements\": []}", MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Pune");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getHospitals().isEmpty());
        assertTrue(result.getMessage().contains("No verified hospitals recorded"));
        assertTrue(result.getMessage().contains("108/112"));

        mockServer.verify();
    }

    @Test
    @DisplayName("9. Ambiguous location: asks for village/town and district before searching places")
    void testGeoapifySearch_AmbiguousLocation_DemandsClarification() {
        String ambiguousNominatim = """
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
                        "lat": 18.995,
                        "lon": 77.258,
                        "display_name": "Alandi, Nanded, Maharashtra",
                        "address": {"county": "Nanded", "state": "Maharashtra", "postcode": "431736"}
                    }
                ]
                """;

        mockServer.expect(requestTo(containsString("nominatim")))
                .andRespond(withSuccess(ambiguousNominatim, MediaType.APPLICATION_JSON));

        HospitalSearchResult result = searchService.searchNearbyHospitals("Alandi, Maharashtra");

        assertNotNull(result);
        assertFalse(result.isFound());
        assertTrue(result.getMessage().contains("Ambiguous location"));
        assertTrue(result.getMessage().contains("district") || result.getMessage().contains("PIN code"));
        assertTrue(result.getHospitals().isEmpty());

        mockServer.verify();
    }

    @Test
    @DisplayName("10. Feature parsing: rejects unnamed POIs, invalid coordinates, and duplicates")
    void testParseGeoapifyFeatures_RejectsUnnamedAndInvalid() throws Exception {
        String json = """
                [
                  {
                    "type": "Feature",
                    "geometry": {"type": "Point", "coordinates": [73.896, 18.675]},
                    "properties": {
                      "name": "Valid Hospital",
                      "formatted": "Alandi, Pune",
                      "lat": 18.675,
                      "lon": 73.896,
                      "categories": ["healthcare.hospital"]
                    }
                  },
                  {
                    "type": "Feature",
                    "geometry": {"type": "Point", "coordinates": [73.896, 18.675]},
                    "properties": {
                      "name": "Valid Hospital",
                      "formatted": "Duplicate entry",
                      "lat": 18.675,
                      "lon": 73.896
                    }
                  },
                  {
                    "type": "Feature",
                    "geometry": {"type": "Point", "coordinates": [73.896, 18.675]},
                    "properties": {
                      "name": "",
                      "formatted": "Unnamed clinic without a name",
                      "lat": 18.675,
                      "lon": 73.896
                    }
                  },
                  {
                    "type": "Feature",
                    "geometry": {"type": "Point", "coordinates": [0.0, 0.0]},
                    "properties": {
                      "name": "Null Island Clinic",
                      "lat": 0.0,
                      "lon": 0.0
                    }
                  }
                ]
                """;

        var features = objectMapper.readTree(json);
        List<HospitalSearchResult.HospitalItem> items =
                searchService.parseGeoapifyFeatures(features, 18.677, 73.898, "Alandi, Pune", 10.0);

        assertEquals(1, items.size());
        assertEquals("Valid Hospital", items.get(0).getName());
    }
}
