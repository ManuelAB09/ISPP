package es.us.meerkat.backend.dto.notifications;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.TipoCanal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta con los datos de una alarma personalizada. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlarmaPersonalizadaResponse {

    /** ID de la alarma. */
    private Long id;

    /** Minutos de antelación configurados. */
    private Integer minutosAntes;

    /** Etiqueta legible: "2 días antes", "1 hora antes", "30 minutos antes"... */
    private String etiqueta;

    /** Canal de notificación: PLATAFORMA, EMAIL o AMBOS. */
    private TipoCanal canal;

    /** Fecha y hora exacta en que se disparará la alarma. */
    private LocalDateTime fechaDisparo;

    /** Si la alarma ya fue disparada por el scheduler. */
    private Boolean disparada;

    /** ID del evento al que pertenece. */
    private Long eventoId;

    /** Título del evento (para mostrar en el listado de alarmas del usuario). */
    private String eventoTitulo;
}
