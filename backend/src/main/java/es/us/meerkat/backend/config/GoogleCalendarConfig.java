package es.us.meerkat.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración de la integración con Google Calendar via OAuth 2.0.
 *
 * <p>Proporciona las constantes de configuración y un método helper para construir el cliente de
 * Calendar para un usuario concreto.
 */
@Configuration
@Slf4j
public class GoogleCalendarConfig {

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    /** Nombre de la aplicación para las llamadas a la API de Google. */
    public static final String APPLICATION_NAME = "Meerkat";

    /** Scope necesario: lectura/escritura del calendario primario. */
    public static final String CALENDAR_SCOPE = CalendarScopes.CALENDAR;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Construye un cliente de Google Calendar autenticado para un usuario concreto.
     *
     * @param accessToken Token de acceso válido del usuario.
     * @param refreshToken Token de refresco del usuario.
     * @return Cliente Calendar listo para usar.
     */
    public Calendar buildCalendarClient(final String accessToken, final String refreshToken)
            throws Exception {

        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        final GoogleCredentials credentials =
                UserCredentials.newBuilder()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret)
                        .setRefreshToken(refreshToken)
                        .build();

        return new Calendar.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
