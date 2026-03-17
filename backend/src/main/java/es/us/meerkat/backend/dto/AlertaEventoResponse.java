package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.TipoAlerta;
import es.us.meerkat.backend.entity.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO que representa una alerta de evento próximo para el usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertaEventoResponse {

    /** Identificador de la alerta. */
    private Long id;

    /** Tipo de alerta. */
    private TipoAlerta tipo;

    /** Mensaje descriptivo de la alerta. */
    private String mensaje;

    /** Si la alerta ha sido leída. */
    private Boolean leida;

    /** Fecha de creación de la alerta. */
    private LocalDateTime createdAt;

    /** ID del evento relacionado. */
    private Long eventoId;

    /** Título del evento relacionado. */
    private String eventoTitulo;

    /** Tipo del evento relacionado. */
    private TipoEvento tipoEvento;

    /** Icono del tipo de evento. */
    private String icono;

    /** Fecha y hora del evento. */
    private LocalDateTime eventoFechaHora;

    /** Nombre de la comunidad del evento. */
    private String comunidadNombre;
}
