package es.us.meerkat.backend.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para solicitar el reenvío del email de verificación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerificationRequest {

    /** Email del usuario que solicita el reenvío. */
    private String email;
}
