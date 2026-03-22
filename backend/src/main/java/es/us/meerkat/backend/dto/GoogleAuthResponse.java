package es.us.meerkat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO retornado tras autenticación con Google. Incluye el AuthResponse y un flag que indica si el
 * frontend debe iniciar el flujo de autorización de Google Classroom.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthResponse {
    private AuthResponse auth;

    /** Si true, el frontend debería llamar a /oauth2/authorize/google-classroom-url con el JWT */
    private Boolean requestClassroomAuth = Boolean.FALSE;
}
