package es.us.meerkat.backend.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña no puede estar vacía")
    @Size(min = 8, max = 128, message = "La nueva contraseña debe tener entre 8 y 128 caracteres")
    private String newPassword;
}
