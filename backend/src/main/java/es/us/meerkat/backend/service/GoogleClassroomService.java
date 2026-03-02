package es.us.meerkat.backend.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import es.us.meerkat.backend.entity.GoogleClassroomConnection;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.GoogleClassroomConnectionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleClassroomService {

    private final GoogleClassroomConnectionRepository connectionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.classroom.client-id}")
    private String clientId;

    @Value("${google.classroom.client-secret}")
    private String clientSecret;

    @Value("${google.classroom.redirect-uri}")
    private String redirectUri;

    /** Guarda o actualiza la conexión OAuth del usuario. */
    public void guardarConexionOAuth(
            Usuario usuario, String accessToken, String refreshToken, long expiresInSeconds) {

        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

        GoogleClassroomConnection connection =
                connectionRepository
                        .findByUsuario(usuario)
                        .orElse(GoogleClassroomConnection.builder().usuario(usuario).build());

        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setExpiresAt(expiresAt);
        connection.setActiva(true);

        connectionRepository.save(connection);
    }

    /** Devuelve el access token válido del usuario. */
    public String getAccessTokenValido(Usuario usuario) {
        GoogleClassroomConnection connection =
                connectionRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Conexión de Google Classroom no encontrada"));

        if (connection.estaExpirado()) {
            refrescarAccessToken(connection);
        }
        return connection.getAccessToken();
    }

    /** Refresca el access token usando el refresh token. */
    public void refrescarAccessToken(GoogleClassroomConnection connection) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", connection.getRefreshToken());
        form.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> tokenResp = restTemplate.postForEntity(tokenUrl, request, Map.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            connection.desactivar();
            connectionRepository.save(connection);
            throw new RuntimeException("Error al refrescar el token de Google Classroom");
        }

        Map<String, Object> body = tokenResp.getBody();
        String newAccessToken = (String) body.get("access_token");
        long expiresIn = ((Integer) body.get("expires_in")).longValue();

        connection.setAccessToken(newAccessToken);
        connection.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        connectionRepository.save(connection);
    }
}
