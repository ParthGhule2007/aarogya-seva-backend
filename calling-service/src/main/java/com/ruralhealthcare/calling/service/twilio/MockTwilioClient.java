package com.ruralhealthcare.calling.service.twilio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "calling.twilio.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockTwilioClient implements TwilioClient {

    private static final Logger log = LoggerFactory.getLogger(MockTwilioClient.class);

    @Override
    public String initiateOutboundCall(String toPhoneNumber, String twimlUrlOrJoinUrl, String statusCallbackUrl) {
        String mockSid = "CA_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String maskedPhone = maskPhoneNumber(toPhoneNumber);
        log.info("[MockTwilioClient] MOCK CALL SIMULATION: Outbound call to [{}] with callback [{}]. Generated Mock SID: [{}]",
                maskedPhone, statusCallbackUrl, mockSid);
        return mockSid;
    }

    @Override
    public void cancelCall(String callSid) {
        log.info("[MockTwilioClient] MOCK CALL SIMULATION: Cancelled call SID [{}]", callSid);
    }

    @Override
    public String fetchCallStatus(String callSid) {
        log.info("[MockTwilioClient] MOCK CALL SIMULATION: Fetching status for SID [{}]", callSid);
        return "completed";
    }

    @Override
    public TwilioCallDetails fetchCallDetails(String callSid) {
        log.info("[MockTwilioClient] MOCK CALL SIMULATION: Fetching details for SID [{}]", callSid);
        return new TwilioCallDetails("in-progress", null, null);
    }

    @Override
    public boolean isMock() {
        return true;
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "[unknown]";
        if (phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "*****" + phone.substring(phone.length() - 4);
    }
}
