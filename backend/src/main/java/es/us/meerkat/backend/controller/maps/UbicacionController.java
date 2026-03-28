package es.us.meerkat.backend.controller.maps;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.maps.UbicacionRequest;
import es.us.meerkat.backend.dto.maps.UbicacionResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.service.maps.UbicacionService;
import lombok.RequiredArgsConstructor;

/** Controlador para manejar las operaciones relacionadas con las ubicaciones. */
@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
public final class UbicacionController {

    /** Servicio para operaciones de ubicación. */
    private final UbicacionService ubicacionService;

    // ===============================
    // CREAR UBICACIÓN
    // ===============================

    /**
     * Crea una nueva ubicación.
     *
     * @param request Datos de la ubicación a crear.
     * @return DTO con la ubicación creada.
     */
    @PostMapping
    public ResponseEntity<UbicacionResponse> crearUbicacion(
            @RequestBody final UbicacionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ubicacionService.crearUbicacion(request));
    }

    // ===============================
    // EDITAR UBICACIÓN
    // ===============================

    /**
     * Edita una ubicación existente.
     *
     * @param ubicacionId Identificador de la ubicación.
     * @param request Nuevos datos de la ubicación.
     * @return DTO actualizado.
     */
    @PutMapping("/{ubicacionId}")
    public ResponseEntity<UbicacionResponse> editarUbicacion(
            @PathVariable final Long ubicacionId, @RequestBody final UbicacionRequest request) {

        return ResponseEntity.ok(ubicacionService.editarUbicacion(ubicacionId, request));
    }

    // ===============================
    // OBTENER UBICACIÓN
    // ===============================

    /**
     * Obtiene una ubicación por su identificador.
     *
     * @param ubicacionId Identificador de la ubicación.
     * @return DTO con los datos de la ubicación.
     */
    @GetMapping("/{ubicacionId}")
    public ResponseEntity<UbicacionResponse> obtenerUbicacion(
            @PathVariable final Long ubicacionId) {

        return ResponseEntity.ok(ubicacionService.obtenerUbicacion(ubicacionId));
    }

    /**
     * Lista todas las ubicaciones registradas.
     *
     * @return Lista de ubicaciones.
     */
    @GetMapping
    public List<Ubicacion> listarTodas() {
        return ubicacionService.obtenerTodas();
    }

    /**
     * Busca sitios de estudio cercanos a una ubicación dada.
     *
     * @param lat Latitud del punto de referencia.
     * @param lon Longitud del punto de referencia.
     * @param radio Radio de búsqueda en metros.
     * @return Lista de ubicaciones cercanas.
     */
    @GetMapping("/buscar-estudio")
    public ResponseEntity<List<UbicacionResponse>> buscarSitiosEstudio(
            @RequestParam Double lat, @RequestParam Double lon, @RequestParam Integer radio) {

        return ResponseEntity.ok(ubicacionService.buscarSitiosEstudio(lat, lon, radio));
    }

    // ===============================
    // ELIMINAR UBICACIÓN
    // ===============================

    /**
     * Elimina una ubicación por su identificador.
     *
     * @param ubicacionId Identificador de la ubicación.
     * @return Respuesta vacía.
     */
    @DeleteMapping("/{ubicacionId}")
    public ResponseEntity<Void> eliminarUbicacion(@PathVariable final Long ubicacionId) {
        ubicacionService.eliminarUbicacion(ubicacionId);
        return ResponseEntity.noContent().build();
    }
}
