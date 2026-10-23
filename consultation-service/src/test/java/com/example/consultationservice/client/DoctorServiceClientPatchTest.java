package com.example.consultationservice.client;

import com.example.consultationservice.dto.DoctorAvailabilityUpdateDTO;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DoctorServiceClientPatchTest {

    private static HttpServer mockDoctorServer;
    private static int mockPort;

    private static final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private static final AtomicReference<String> receivedPath = new AtomicReference<>();
    private static final AtomicReference<String> receivedHeader = new AtomicReference<>();
    private static final AtomicReference<String> receivedBody = new AtomicReference<>();

    @BeforeAll
    static void startMockDoctorService() throws Exception {
        mockDoctorServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockDoctorServer.getAddress().getPort();

        mockDoctorServer.createContext("/api/doctors/1/availability", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedHeader.set(exchange.getRequestHeaders().getFirst("X-Service-Source"));

            try (InputStream is = exchange.getRequestBody()) {
                receivedBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }

            byte[] response = new byte[0];
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().close();
        });

        mockDoctorServer.start();
    }

    @AfterAll
    static void stopMockDoctorService() {
        if (mockDoctorServer != null) {
            mockDoctorServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("doctor-service.url", () -> "http://localhost:" + mockPort);
    }

    @Autowired
    private DoctorServiceClient doctorServiceClient;

    @Test
    @DisplayName("Verify OpenFeign sends HTTP PATCH with Apache HttpClient 5 and X-Service-Source header")
    void testFeignPatchMethodExecution() {
        DoctorAvailabilityUpdateDTO updateDTO = new DoctorAvailabilityUpdateDTO("BUSY");

        ResponseEntity<Void> response = doctorServiceClient.updateDoctorAvailability(1L, updateDTO, "Consultation-Service");

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        assertEquals("PATCH", receivedMethod.get(), "HTTP method must be PATCH");
        assertEquals("/api/doctors/1/availability", receivedPath.get());
        assertEquals("Consultation-Service", receivedHeader.get(), "Header X-Service-Source must be Consultation-Service");
        assertNotNull(receivedBody.get());
        assertTrue(receivedBody.get().contains("BUSY"), "Request body must contain 'BUSY'");
    }
}
