package com.ruralhealthcare.calling.security;

import com.ruralhealthcare.calling.config.CallingProperties;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TwilioSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(TwilioSignatureValidator.class);

    private final CallingProperties properties;

    public TwilioSignatureValidator(CallingProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates incoming Twilio webhook HTTP request signature against the configured Twilio auth token.
     * Checks multiple URL candidates (direct URL, X-Forwarded headers, and configured webhookBaseUrl).
     *
     * @param request the HttpServletRequest
     * @param params form POST parameters submitted by Twilio
     * @param signature the value of the X-Twilio-Signature HTTP header
     * @return true if signature is valid or if mock mode bypasses validation
     */
    public boolean validate(HttpServletRequest request, Map<String, String> params, String signature) {
        if (properties.getTwilio().isMockMode()) {
            if ("mock_valid_signature".equals(signature) || signature == null || signature.isBlank()) {
                log.debug("[TwilioSignatureValidator] Mock mode active: accepting webhook request");
                return true;
            }
        }

        if (signature == null || signature.isBlank()) {
            log.warn("[TwilioSignatureValidator] Missing X-Twilio-Signature header");
            return false;
        }

        String authToken = properties.getTwilio().getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            log.error("[TwilioSignatureValidator] Twilio Auth Token is not configured");
            return false;
        }

        List<String> candidateUrls = buildCandidateUrls(request);
        try {
            RequestValidator validator = new RequestValidator(authToken);
            for (String url : candidateUrls) {
                if (validator.validate(url, params, signature)) {
                    log.debug("[TwilioSignatureValidator] Signature verified against URL [{}]", url);
                    return true;
                }
            }
            log.warn("[TwilioSignatureValidator] Invalid Twilio signature across [{}] candidate URL(s)", candidateUrls.size());
            return false;
        } catch (Exception ex) {
            log.error("[TwilioSignatureValidator] Error validating Twilio signature: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Legacy validation method accepting a raw URL string.
     */
    public boolean validate(String requestUrl, Map<String, String> params, String signature) {
        if (properties.getTwilio().isMockMode()) {
            if ("mock_valid_signature".equals(signature) || signature == null || signature.isBlank()) {
                return true;
            }
        }

        if (signature == null || signature.isBlank()) {
            return false;
        }

        String authToken = properties.getTwilio().getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            return false;
        }

        try {
            RequestValidator validator = new RequestValidator(authToken);
            if (validator.validate(requestUrl, params, signature)) {
                return true;
            }
            // Also test HTTPS equivalent if direct URL was HTTP
            if (requestUrl.startsWith("http://")) {
                String httpsUrl = "https://" + requestUrl.substring(7);
                if (validator.validate(httpsUrl, params, signature)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.error("[TwilioSignatureValidator] Error validating signature: {}", ex.getMessage());
            return false;
        }
    }

    List<String> buildCandidateUrls(HttpServletRequest request) {
        Set<String> candidates = new LinkedHashSet<>();

        String path = request.getRequestURI();
        String query = (request.getQueryString() != null && !request.getQueryString().isBlank()) ? "?" + request.getQueryString() : "";

        // 1. Direct request URL
        String directUrl = request.getRequestURL().toString();
        addUrlVariations(candidates, directUrl + query);

        // 2. Reconstructed from request scheme, serverName, serverPort
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        addUrlVariations(candidates, scheme + "://" + serverName + path + query);
        if (serverPort > 0 && serverPort != 80 && serverPort != 443) {
            addUrlVariations(candidates, scheme + "://" + serverName + ":" + serverPort + path + query);
        }

        // 3. Reconstructed from Host header
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            String hostClean = hostHeader.trim();
            addUrlVariations(candidates, "https://" + hostClean + path + query);
            addUrlVariations(candidates, "http://" + hostClean + path + query);
            if (hostClean.contains(":")) {
                String hostNoPort = hostClean.substring(0, hostClean.indexOf(':'));
                addUrlVariations(candidates, "https://" + hostNoPort + path + query);
                addUrlVariations(candidates, "http://" + hostNoPort + path + query);
            }
        }

        // 4. Reconstructed from X-Forwarded-Proto and X-Forwarded-Host
        String proto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            String p = (proto != null && !proto.isBlank()) ? proto.trim() : "https";
            addUrlVariations(candidates, p + "://" + forwardedHost.trim() + path + query);
            if (forwardedHost.contains(":")) {
                String hostNoPort = forwardedHost.substring(0, forwardedHost.indexOf(':'));
                addUrlVariations(candidates, p + "://" + hostNoPort + path + query);
            }
        }

        // 5. Reconstructed using configured calling.twilio.webhook-base-url
        String configuredBase = properties.getTwilio().getWebhookBaseUrl();
        if (configuredBase != null && !configuredBase.isBlank() && configuredBase.startsWith("http")) {
            String base = configuredBase.trim();
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            addUrlVariations(candidates, base + path + query);
            if (base.startsWith("http://")) {
                addUrlVariations(candidates, "https://" + base.substring(7) + path + query);
            }
        }

        return new ArrayList<>(candidates);
    }

    private void addUrlVariations(Set<String> candidates, String url) {
        if (url == null || url.isBlank()) return;
        String clean = url.trim();
        candidates.add(clean);

        if (clean.startsWith("http://")) {
            candidates.add("https://" + clean.substring(7));
        }

        try {
            java.net.URI uri = java.net.URI.create(clean);
            if (uri.getPort() > 0) {
                String withoutPort = uri.getScheme() + "://" + uri.getHost()
                        + (uri.getRawPath() != null ? uri.getRawPath() : "")
                        + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                candidates.add(withoutPort);
                if (withoutPort.startsWith("http://")) {
                    candidates.add("https://" + withoutPort.substring(7));
                }
            } else if ("https".equalsIgnoreCase(uri.getScheme())) {
                String with443 = uri.getScheme() + "://" + uri.getHost() + ":443"
                        + (uri.getRawPath() != null ? uri.getRawPath() : "")
                        + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                candidates.add(with443);
            }
        } catch (Exception ignored) {
        }
    }
}
