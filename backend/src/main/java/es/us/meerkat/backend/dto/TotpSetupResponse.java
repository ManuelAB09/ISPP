package es.us.meerkat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta al solicitar la configuración TOTP: secret y otpauth URL para QR. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotpSetupResponse {
    private String secret;
    private String otpauthUrl;
}
