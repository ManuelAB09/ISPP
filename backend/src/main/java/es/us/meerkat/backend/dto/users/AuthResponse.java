package es.us.meerkat.backend.dto.users;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta tras autenticación exitosa.
 *
 * <p>Corresponde al schema AuthResponse del OpenAPI. Devuelve accessToken, refreshToken y datos del
 * usuario.
 */
@Data
@Builder
public class AuthResponse {

    /** Token JWT de acceso. Válido 24 horas. */
    private String accessToken;

    /** Tipo de token. Siempre "Bearer". */
    @Builder.Default private String tokenType = "Bearer";

    /** Tiempo de expiración en segundos. */
    @Builder.Default private long expiresIn = 86400L;

    /** Datos del usuario autenticado. */
    private UserDetailResponse user;
}
