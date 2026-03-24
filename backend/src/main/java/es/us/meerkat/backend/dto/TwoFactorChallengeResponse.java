package es.us.meerkat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta cuando el login requiere 2FA: devuelve flag y tempToken. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorChallengeResponse {
    private Boolean twoFactorRequired = Boolean.TRUE;
    private String tempToken;
}
