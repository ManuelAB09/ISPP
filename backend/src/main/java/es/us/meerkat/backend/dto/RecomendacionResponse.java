package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
//  RecomendacionResponse
// ============================================================
// Coloca esta clase en su propio archivo o como clase pública si lo prefieres
// Se incluye aquí como referencia de todos los DTOs juntos
// ============================================================

/** Response con los datos de una recomendación para el frontend. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionResponse {

    private Long id;

    /** PROFESOR | CONTENIDO | CUESTIONARIO | COMUNIDAD | EVENTO */
    private String tipo;

    /** ID del objeto recomendado (tutor, comunidad, quiz...) */
    private Long communityId; // Mantenemos el nombre existente por compatibilidad

    private String nombre;
    private String descripcion;
    private String imagenUrl;

    /** Puntuación de relevancia 0-100 */
    private Double relevancia;

    /** Texto legible que explica por qué se recomienda */
    private String motivo;

    private Boolean vista;
    private Boolean esFavorable; // null=sin feedback, true=útil, false=no útil

    private LocalDateTime createdAt;
}
