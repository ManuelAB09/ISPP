package es.us.meerkat.backend.dto.google;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para la petición de autenticación con Google. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {
    /** Token ID de Google (JWT) obtenido por el cliente. */
    private String idToken;

    /** Si se solicita acceso a Google Classroom tras el login. */
    private Boolean requestClassroomAccess = Boolean.FALSE;
}
