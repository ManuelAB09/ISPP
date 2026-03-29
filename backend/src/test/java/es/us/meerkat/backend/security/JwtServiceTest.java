package es.us.meerkat.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService jwtService;

    // Base64-encoded 256-bit key for testing
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWM=";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        Field secretField = JwtService.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtService, TEST_SECRET);

        Field expirationField = JwtService.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, 86400000L);
    }

    @Test
    void generateTokenShouldReturnValidJwt() {
        String token = jwtService.generateToken("user@test.es");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.es");
    }

    @Test
    void isTokenValidShouldReturnTrueForValidAccessToken() {
        String token = jwtService.generateToken("user@test.es");

        assertThat(jwtService.isTokenValid(token, "user@test.es")).isTrue();
    }

    @Test
    void isTokenValidShouldReturnFalseForWrongEmail() {
        String token = jwtService.generateToken("user@test.es");

        assertThat(jwtService.isTokenValid(token, "other@test.es")).isFalse();
    }

    @Test
    void isTokenValidShouldRejectPasswordResetToken() {
        String resetToken = jwtService.generatePasswordResetToken("user@test.es");

        assertThat(jwtService.isTokenValid(resetToken, "user@test.es")).isFalse();
    }

    @Test
    void generatePasswordResetTokenShouldReturnValidToken() {
        String token = jwtService.generatePasswordResetToken("user@test.es");

        assertThat(token).isNotBlank();
        String email = jwtService.validatePasswordResetToken(token);
        assertThat(email).isEqualTo("user@test.es");
    }

    @Test
    void validatePasswordResetTokenShouldRejectAccessToken() {
        String accessToken = jwtService.generateToken("user@test.es");

        assertThatThrownBy(() -> jwtService.validatePasswordResetToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void isTokenValidShouldRejectExpiredToken() throws Exception {
        // Set expiration to 0 to create an already-expired token
        Field expirationField = JwtService.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, 0L);

        String token = jwtService.generateToken("user@test.es");

        // JJWT throws ExpiredJwtException during parsing, which means the token is rejected
        assertThatThrownBy(() -> jwtService.isTokenValid(token, "user@test.es"))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractEmailShouldThrowForTamperedToken() {
        String token = jwtService.generateToken("user@test.es");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractEmail(tampered)).isInstanceOf(Exception.class);
    }
}
