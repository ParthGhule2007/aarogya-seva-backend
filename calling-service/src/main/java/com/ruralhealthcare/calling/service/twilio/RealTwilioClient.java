package com.ruralhealthcare.calling.service.twilio;

import com.ruralhealthcare.calling.config.CallingProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
@ConditionalOnProperty(name = "calling.twilio.mock-mode", havingValue = "false")
public class RealTwilioClient implements TwilioClient {

    private static final Logger log = LoggerFactory.getLogger(RealTwilioClient.class);

    private final CallingProperties properties;

    public RealTwilioClient(CallingProperties properties) {
        this.properties = properties;
        String accountSid = properties.getTwilio().getAccountSid();
        String authToken = properties.getTwilio().getAuthToken();
        if (accountSid != null && authToken != null) {
            Twilio.init(accountSid, authToken);
            log.info("[RealTwilioClient] Initialized Twilio SDK with Account SID [{}]", accountSid);
        }
    }

    @Override
    public String initiateOutboundCall(String toPhoneNumber, String twimlUrlOrJoinUrl, String statusCallbackUrl) {
        String maskedPhone = maskPhoneNumber(toPhoneNumber);
        log.info("[RealTwilioClient] Initiating outbound live call to [{}]", maskedPhone);

        if (statusCallbackUrl != null && !statusCallbackUrl.startsWith("https://")) {
            log.warn("[RealTwilioClient] WARNING: statusCallbackUrl [{}] does not use HTTPS. Twilio cloud cannot dispatch status callbacks to local or non-public HTTP endpoints. Ensure CALLING_PUBLIC_BASE_URL is configured with an active public HTTPS tunnel (e.g. ngrok).", statusCallbackUrl);
        }

        String fromNumber = properties.getTwilio().getPhoneNumber();
        com.twilio.rest.api.v2010.account.CallCreator creator;

        if (twimlUrlOrJoinUrl != null && (twimlUrlOrJoinUrl.startsWith("http://") || twimlUrlOrJoinUrl.startsWith("https://"))) {
            log.info("[RealTwilioClient] Placing call using public TwiML URL [{}] (bypassing inline Twiml trial limitations)", twimlUrlOrJoinUrl);
            creator = Call.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromNumber),
                    URI.create(twimlUrlOrJoinUrl)
            );
        } else {
            log.warn("[RealTwilioClient] Placing call with inline Twiml parameter: [{}]", twimlUrlOrJoinUrl);
            String safeJoinUrl = twimlUrlOrJoinUrl != null ? twimlUrlOrJoinUrl.replace("&", "&amp;").replace("\"", "&quot;") : "";
            String twimlXml = "<Response><Connect><Stream url=\"" + safeJoinUrl + "\"/></Connect></Response>";
            creator = Call.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromNumber),
                    new Twiml(twimlXml)
            );
        }

        if (statusCallbackUrl != null && !statusCallbackUrl.isBlank()) {
            try {
                creator.setStatusCallback(URI.create(statusCallbackUrl));
            } catch (Exception ex) {
                log.warn("[RealTwilioClient] Skipping invalid statusCallbackUrl [{}]: {}", statusCallbackUrl, ex.getMessage());
            }
        }

        try {
            Call call = creator.create();
            log.info("[RealTwilioClient] Outbound call successfully placed. SID: [{}]", call.getSid());
            return call.getSid();
        } catch (com.twilio.exception.ApiException ex) {
            log.error("[RealTwilioClient] Twilio API call failed for destination [{}] (Code: {}): {}",
                    maskedPhone, ex.getCode(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void cancelCall(String callSid) {
        log.info("[RealTwilioClient] Terminating live call SID [{}]", callSid);
        try {
            Call.updater(callSid)
                    .setStatus(Call.UpdateStatus.CANCELED)
                    .update();
        } catch (Exception ex) {
            log.warn("[RealTwilioClient] Exception canceling call SID [{}]: {}", callSid, ex.getMessage());
        }
    }

    @Override
    public String fetchCallStatus(String callSid) {
        TwilioCallDetails details = fetchCallDetails(callSid);
        return details != null ? details.status() : null;
    }

    @Override
    public TwilioCallDetails fetchCallDetails(String callSid) {
        if (callSid == null || callSid.isBlank()) {
            return new TwilioCallDetails(null, null, null);
        }
        if (callSid.startsWith("CA_mock_")) {
            return new TwilioCallDetails("in-progress", null, null);
        }
        log.info("[RealTwilioClient] Fetching authoritative call details from Twilio for SID [{}]", callSid);
        try {
            Call call = Call.fetcher(callSid).fetch();
            String status = call.getStatus() != null ? call.getStatus().toString() : null;
            return new TwilioCallDetails(status, null, null);
        } catch (com.twilio.exception.ApiException ex) {
            log.warn("[RealTwilioClient] Twilio error fetching details for SID [{}] (Code: {}): {}", callSid, ex.getCode(), ex.getMessage());
            return new TwilioCallDetails("failed", ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("[RealTwilioClient] Exception fetching details for SID [{}]: {}", callSid, ex.getMessage());
            return new TwilioCallDetails(null, null, ex.getMessage());
        }
    }

    @Override
    public boolean isMock() {
        return false;
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "[unknown]";
        if (phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "*****" + phone.substring(phone.length() - 4);
    }
}
