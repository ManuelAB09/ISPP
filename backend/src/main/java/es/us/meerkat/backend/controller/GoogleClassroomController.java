package es.us.meerkat.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/oauth2")
public class GoogleClassroomController {

    @Value("${google.classroom.client-id}")
    private String clientId;

    @Value("${google.classroom.client-secret}")
    private String clientSecret;

    @Value("${google.classroom.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/authorize/google-classroom")
    public ResponseEntity<Void> authorize() {
        String scope =
                URLEncoder.encode(
                        "https://www.googleapis.com/auth/classroom.courses.readonly",
                        StandardCharsets.UTF_8);
        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id="
                        + clientId
                        + "&redirect_uri="
                        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope="
                        + scope
                        + "&access_type=offline"
                        + "&prompt=consent";

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback/google-classroom")
    public ResponseEntity<String> callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error)
            throws Exception {
        if (error != null) {
            String errHtml =
                    "<html><body><script>window.opener.postMessage({error:'"
                            + error
                            + "'}, '*');window.close();</script></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(errHtml);
        }
        if (code == null) {
            String errHtml =
                    "<html><body><script>window.opener.postMessage({error:'no_code'},"
                            + " '*');window.close();</script></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(errHtml);
        }

        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> tokenResp = rest.postForEntity(tokenUrl, request, String.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            String errHtml =
                    "<html><body><script>window.opener.postMessage({error:'token_error'},"
                            + " '*');window.close();</script></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(errHtml);
        }

        JsonNode tokenJson = mapper.readTree(tokenResp.getBody());
        String accessToken = tokenJson.path("access_token").asText(null);
        if (accessToken == null) {
            String errHtml =
                    "<html><body><script>window.opener.postMessage({error:'no_access_token'},"
                            + " '*');window.close();</script></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(errHtml);
        }

        // Call Classroom API to list courses
        String coursesUrl = "https://classroom.googleapis.com/v1/courses";
        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(accessToken);
        HttpEntity<Void> coursesReq = new HttpEntity<>(auth);

        ResponseEntity<String> coursesResp =
                rest.exchange(coursesUrl, HttpMethod.GET, coursesReq, String.class);
        String payload = "{}";
        if (coursesResp.getStatusCode().is2xxSuccessful()) {
            payload = coursesResp.getBody();
        }

        String safe = payload.replace("</", "<\\/");
        String html =
                "<html><body><script>window.opener.postMessage({courses:"
                        + safe
                        + "}, '*');window.close();</script></body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
