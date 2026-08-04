package com.vincent.msyep.modules.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Sends WhatsApp documents via the Meta WhatsApp Cloud API.
 * Until credentials are configured it logs the intent (the PDF is generated and ready to send),
 * so the whole flow works end-to-end the moment the gateway is wired.
 *
 * Configure:
 *   whatsapp.token             = permanent access token
 *   whatsapp.phone-number-id   = WhatsApp Business phone number id
 *   whatsapp.api-version       = graph API version (default v20.0)
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final String token;
    private final String phoneNumberId;
    private final String apiVersion;
    private final HttpClient http = HttpClient.newHttpClient();

    public WhatsAppService(@Value("${whatsapp.token:}") String token,
                           @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
                           @Value("${whatsapp.api-version:v20.0}") String apiVersion) {
        this.token = token;
        this.phoneNumberId = phoneNumberId;
        this.apiVersion = apiVersion;
    }

    public boolean configured() {
        return StringUtils.hasText(token) && StringUtils.hasText(phoneNumberId);
    }

    /**
     * Send the entrance-test result PDF to a student's WhatsApp number.
     * Returns true if actually dispatched to the gateway.
     */
    public boolean sendResultPdf(String phone, String name, String outcome, int score, int total,
                                 byte[] pdf, String filename) {
        String caption = "MSYEP Entrance Test Result — " + nz(name)
                + ": " + score + "/" + total + " (" + outcome + ").";
        if (!configured()) {
            log.info("WhatsApp not configured — result PDF ({} bytes) ready to send to {} [{}].",
                    pdf == null ? 0 : pdf.length, mask(phone), caption);
            return false;
        }
        try {
            String mediaId = uploadMedia(pdf, filename);
            if (mediaId == null) return false;
            sendDocument(phone, mediaId, filename, caption);
            log.info("WhatsApp result sent to {}", mask(phone));
            return true;
        } catch (Exception e) {
            log.warn("WhatsApp send failed for {}: {}", mask(phone), e.getMessage());
            return false;
        }
    }

    /** Upload the PDF to the WhatsApp media endpoint, returns the media id. */
    private String uploadMedia(byte[] pdf, String filename) throws Exception {
        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/media";
        String boundary = "----msyep" + System.identityHashCode(pdf);
        var body = new java.io.ByteArrayOutputStream();
        String pre = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"messaging_product\"\r\n\r\nwhatsapp\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"type\"\r\n\r\napplication/pdf\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: application/pdf\r\n\r\n";
        body.write(pre.getBytes(StandardCharsets.UTF_8));
        body.write(pdf);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return extract(resp.body(), "\"id\"");
    }

    /** Send a document message referencing the uploaded media id. */
    private void sendDocument(String phone, String mediaId, String filename, String caption) throws Exception {
        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
        String json = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + phone.replaceAll("[^0-9]", "")
                + "\",\"type\":\"document\",\"document\":{\"id\":\"" + mediaId
                + "\",\"filename\":\"" + filename + "\",\"caption\":\"" + caption.replace("\"", "'") + "\"}}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String extract(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        int start = json.indexOf('"', json.indexOf(':', i) + 1) + 1;
        int end = json.indexOf('"', start);
        return start > 0 && end > start ? json.substring(start, end) : null;
    }

    private static String mask(String v) {
        if (!StringUtils.hasText(v) || v.length() < 4) return "****";
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
