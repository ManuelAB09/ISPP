package es.us.meerkat.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta paginada de asistencias a eventos.
 *
 * <p>Contiene una lista de asistencias registradas a un evento y la información de paginación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceListResponse {

    /** Lista de asistencias. */
    private List<AttendanceResponse> content;

    /** Información de paginación. */
    private PageInfo page;
}
