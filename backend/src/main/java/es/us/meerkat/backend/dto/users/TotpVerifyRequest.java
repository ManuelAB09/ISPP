package es.us.meerkat.backend.dto.users;

import lombok.Data;

/** DTO para verificar códigos TOTP (enable/disable). */
@Data
public class TotpVerifyRequest {
    private String code;
}
