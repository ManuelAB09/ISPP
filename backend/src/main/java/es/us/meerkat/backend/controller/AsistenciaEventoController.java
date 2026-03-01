package es.us.meerkat.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.AttendanceResponse;
import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.service.AsistenciaEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador para manejar las operaciones relacionadas con la asistencia a
 * eventos.
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/attendance")
@RequiredArgsConstructor
@Tag(name = "Asistencia", description = "Gestión de asistencia a eventos")
public class AsistenciaEventoController {

        /** Servicio para operaciones de asistencia a eventos. */
        private final AsistenciaEventoService asistenciaService;

        // ===============================
        // CONFIRMAR ASISTENCIA
        // ===============================

        /**
         * Confirma la asistencia de un usuario a un evento.
         *
         * @param eventId   Identificador del evento.
         * @param usuarioId Identificador del usuario.
         * @return La asistencia confirmada.
         */
        @PostMapping
        @Operation(summary = "Confirmar asistencia", description = "Confirma que un usuario asistirá a un evento")
        public ResponseEntity<AttendanceResponse> confirmarAsistencia(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId,
                        @Parameter(description = "ID del usuario") @RequestParam final Long usuarioId) {

                final AsistenciaEvento asistencia = asistenciaService.confirmarAsistencia(eventId, usuarioId);
                return ResponseEntity.status(HttpStatus.CREATED).body(asistencia.toDTO());
        }

        // ===============================
        // OBTENER ASISTENCIA PROPIA
        // ===============================

        /**
         * Obtiene la asistencia del usuario actual a un evento.
         *
         * @param eventId   Identificador del evento.
         * @param usuarioId Identificador del usuario actual.
         * @return La asistencia del usuario.
         */
        @GetMapping("/me")
        @Operation(summary = "Obtener asistencia propia", description = "Devuelve el estado de asistencia del usuario al evento")
        public ResponseEntity<AttendanceResponse> obtenerAsistenciaPropia(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId,
                        @Parameter(description = "ID del usuario") @RequestParam final Long usuarioId) {
                try {
                        return ResponseEntity.ok(asistenciaService.obtenerAsistencia(eventId, usuarioId).toDTO());
                } catch (final RuntimeException e) {
                        if ("Asistencia no encontrada".equals(e.getMessage())) {
                                return ResponseEntity.noContent().build();
                        }
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
        }

        /**
         * Cancela la asistencia del usuario actual a un evento.
         *
         * @param eventId   Identificador del evento.
         * @param usuarioId Identificador del usuario actual.
         * @return Respuesta vacía.
         */
        @DeleteMapping("/me")
        @Operation(summary = "Cancelar asistencia propia", description = "Retira la confirmación de asistencia del usuario")
        public ResponseEntity<Void> cancelarAsistenciaPropia(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId,
                        @Parameter(description = "ID del usuario") @RequestParam final Long usuarioId) {
                try {
                        asistenciaService.cancelarAsistencia(eventId, usuarioId);
                        return ResponseEntity.noContent().build();
                } catch (final RuntimeException e) {
                        if ("Asistencia no encontrada".equals(e.getMessage())) {
                                return ResponseEntity.noContent().build();
                        }
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
        }

        // ===============================
        // OBTENER ASISTENCIAS DEL EVENTO
        // ===============================

        /**
         * Obtiene todas las asistencias de un evento.
         *
         * @param eventId Identificador del evento.
         * @return Lista de asistencias del evento.
         */
        @GetMapping
        @Operation(summary = "Listar asistencias", description = "Obtiene todas las asistencias registradas a un evento")
        public ResponseEntity<List<AttendanceResponse>> obtenerAsistenciasEvento(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId) {

                return ResponseEntity.ok(
                                asistenciaService.obtenerAsistenciasEvento(eventId).stream()
                                                .map(AsistenciaEvento::toDTO)
                                                .collect(Collectors.toList()));
        }

        /**
         * Obtiene todas las asistencias confirmadas de un evento.
         *
         * @param eventId Identificador del evento.
         * @return Lista de asistencias confirmadas.
         */
        @GetMapping("/confirmed")
        @Operation(summary = "Listar asistentes confirmados", description = "Devuelve solo los usuarios que han confirmado su asistencia")
        public ResponseEntity<List<AttendanceResponse>> obtenerAsistentesConfirmados(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId) {

                return ResponseEntity.ok(
                                asistenciaService.obtenerAsistentesConfirmados(eventId).stream()
                                                .map(AsistenciaEvento::toDTO)
                                                .collect(Collectors.toList()));
        }

        /**
         * Obtiene el número total de asistentes confirmados a un evento.
         *
         * @param eventId Identificador del evento.
         * @return Número de asistentes confirmados.
         */
        @GetMapping("/count")
        @Operation(summary = "Contar asistentes", description = "Devuelve el número total de asistentes confirmados")
        public ResponseEntity<Long> contarAsistentes(
                        @PathVariable @Parameter(description = "ID del evento") final Long eventId) {

                return ResponseEntity.ok(asistenciaService.contarAsistentesConfirmados(eventId));
        }
}
