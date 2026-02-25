package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.UbicacionRequest;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.repository.UbicacionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio relacionada con las ubicaciones.
 *
 * <p>
 * Permite crear, editar y consultar ubicaciones geográficas asociadas a
 * eventos.
 */
@Service
@RequiredArgsConstructor
public class UbicacionService {

    /** Repositorio para acceder a las ubicaciones. */
    private final UbicacionRepository ubicacionRepository;

    // ===============================
    // CREAR UBICACIÓN
    // ===============================

    /**
     * Crea una nueva ubicación.
     *
     * @param requestParam Datos de la ubicación.
     * @return DTO con los datos de la ubicación creada.
     */
    @Transactional
    public UbicacionResponse crearUbicacion(final UbicacionRequest requestParam) {

        final Ubicacion ubicacion = Ubicacion.builder()
                .nombre(requestParam.getNombre())
                .direccion(requestParam.getDireccion())
                .latitud(requestParam.getLatitud())
                .longitud(requestParam.getLongitud())
                .build();

        ubicacionRepository.save(ubicacion);

        return mapToResponse(ubicacion);
    }

    // ===============================
    // EDITAR UBICACIÓN
    // ===============================

    /**
     * Edita una ubicación existente.
     *
     * @param ubicacionIdParam Identificador de la ubicación.
     * @param requestParam     Nuevos datos.
     * @return DTO actualizado.
     */
    @Transactional
    public UbicacionResponse editarUbicacion(
            final Long ubicacionIdParam, final UbicacionRequest requestParam) {

        final Ubicacion ubicacion = ubicacionRepository
                .findById(ubicacionIdParam)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));

        ubicacion.setNombre(requestParam.getNombre());
        ubicacion.setDireccion(requestParam.getDireccion());
        ubicacion.setLatitud(requestParam.getLatitud());
        ubicacion.setLongitud(requestParam.getLongitud());

        ubicacionRepository.save(ubicacion);

        return mapToResponse(ubicacion);
    }

    // ===============================
    // OBTENER UBICACIÓN
    // ===============================

    /**
     * Obtiene una ubicación por su ID.
     *
     * @param ubicacionIdParam Identificador.
     * @return DTO con los datos.
     */
    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacion(final Long ubicacionIdParam) {

        final Ubicacion ubicacion = ubicacionRepository
                .findById(ubicacionIdParam)
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));

        return mapToResponse(ubicacion);
    }

    /**
     * Obtiene todas las ubicaciones registradas.
     *
     * @return Lista de ubicaciones.
     */
    @Transactional(readOnly = true)
    public List<Ubicacion> obtenerTodas() {
        return ubicacionRepository.findAll();
    }

    // ===============================
    // ELIMINAR UBICACIÓN
    // ===============================

    /**
     * Elimina una ubicación por su ID.
     *
     * @param ubicacionIdParam Identificador.
     */
    @Transactional
    public void eliminarUbicacion(final Long ubicacionIdParam) {
        if (!ubicacionRepository.existsById(ubicacionIdParam)) {
            throw new RuntimeException("Ubicación no encontrada");
        }

        ubicacionRepository.deleteById(ubicacionIdParam);
    }

    // ===============================
    // MAPPER
    // ===============================

    /** Mapea entidad a DTO. */
    private UbicacionResponse mapToResponse(final Ubicacion ubicacionParam) {
        return UbicacionResponse.builder()
                .id(ubicacionParam.getId())
                .nombre(ubicacionParam.getNombre())
                .direccion(ubicacionParam.getDireccion())
                .latitud(ubicacionParam.getLatitud())
                .longitud(ubicacionParam.getLongitud())
                .build();
    }
}
