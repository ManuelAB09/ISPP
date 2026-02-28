package es.us.meerkat.backend.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OverpassClient {
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String URL = "https://overpass-api.de/api/interpreter";

    public String ejecutar(String query) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> request = new HttpEntity<>(query, headers);

            return restTemplate.postForObject(URL, request, String.class);

        } catch (RestClientException ex) {
            throw new RestClientException("No se pudo conectar con Overpass API");
        }
    }
}
