package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio relacionada con los eventos.
 *
 * <p>Incluye creación, edición, cancelación y obtención de eventos.
 */
@Service
@RequiredArgsConstructor
public class EventoService {

    /** Repositorio para acceder a la información de eventos. */
    private final EventoRepository eventoRepository;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Repositorio para acceder a la información de comunidades. */
    private final ComunidadRepository comunidadRepository;

    /** Repositorio para acceder a la información de miembros de comunidad. */
    private final MiembroComunidadRepository miembroComunidadRepository;

    /** Repositorio para acceder a la información de ubicaciones. */
    private final UbicacionRepository ubicacionRepository;

    // ===============================
    // CREAR EVENTO
    // ===============================

    /**
     * Crea un nuevo evento asociado a una comunidad.
     *
     * @param creadorId Identificador del usuario creador.
     * @param comunidadId Identificador de la comunidad.
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaHoraParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @param enlaceVirtualParam Enlace virtual (si aplica).
     * @param visibleMapaParam Si es visible en el mapa.
     * @param ubicacionId ID de la ubicación (para eventos presenciales).
     * @return El evento creado.
     */
    @Transactional
    public Evento crearEvento(
            final Long creadorId,
            final Long comunidadId,
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaHoraParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam,
            final String enlaceVirtualParam,
            final Boolean visibleMapaParam,
            final Long ubicacionId) {

        final Usuario creador =
                usuarioRepository
                        .findById(creadorId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        final Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        // Verificar que el usuario es miembro de la comunidad
        boolean esMiembro =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(creadorId, comunidadId)
                        .isPresent();
        if (!esMiembro) {
            throw new RuntimeException(
                    "No puedes crear eventos en una comunidad a la que no perteneces");
        }

        // Validar que la fecha de inicio sea anterior a la fecha de fin
        if (fechaFinParam != null && fechaHoraParam.isAfter(fechaFinParam)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        final Evento evento = new Evento();
        evento.crear(
                tituloParam,
                descripcionParam,
                fechaHoraParam,
                fechaFinParam,
                aforoParam,
                queLlevarParam,
                esVirtualParam,
                privadoParam);
        evento.setCreador(creador);
        evento.setComunidad(comunidad);
        evento.setEnlaceVirtual(enlaceVirtualParam);
        evento.setVisibleMapa(visibleMapaParam != null ? visibleMapaParam : true);

        // Establecer ubicación si se proporcionó
        if (ubicacionId != null) {
            final Ubicacion ubicacion = ubicacionRepository.findById(ubicacionId)
                    .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
            evento.setUbicacion(ubicacion);
        }

        return eventoRepository.save(evento);
    }

    // ===============================
    // EDITAR EVENTO
    // ===============================

    /**
     * Edita un evento existente.
     *
     * @param eventoIdParam Identificador del evento.
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaInicioParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @param ubicacionId ID de la ubicación (para eventos presenciales).
     * @return El evento actualizado.
     */
    @Transactional
    public Evento editarEvento(
            final Long eventoIdParam,
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaInicioParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam,
            final Long ubicacionId) {

        // Validar que la fecha de inicio sea anterior a la fecha de fin
        if (fechaFinParam != null && fechaInicioParam.isAfter(fechaFinParam)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        evento.editar(
                tituloParam,
                descripcionParam,
                fechaInicioParam,
                fechaFinParam,
                aforoParam,
                queLlevarParam,
                esVirtualParam,
                privadoParam);

        // Actualizar ubicación
        if (ubicacionId != null) {
            final Ubicacion ubicacion = ubicacionRepository.findById(ubicacionId)
                    .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
            evento.setUbicacion(ubicacion);
        } else if (Boolean.TRUE.equals(esVirtualParam)) {
            evento.setUbicacion(null);
        }

        return eventoRepository.save(evento);
    }

    // ===============================
    // CANCELAR EVENTO
    // ===============================

    /**
     * Cancela un evento existente.
     *
     * @param eventoIdParam Identificador del evento.
     * @param motivoParam Motivo de la cancelación.
     * @return El evento cancelado.
     */
    @Transactional
    public Evento cancelarEvento(final Long eventoIdParam, final String motivoParam) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        evento.cancelar(motivoParam);
        return eventoRepository.save(evento);
    }

    // ===============================
    // OBTENER EVENTO
    // ===============================

    /**
     * Obtiene un evento por su ID.
     *
     * @param eventoIdParam Identificador del evento.
     * @return El evento encontrado.
     */
    public Evento obtenerEvento(final Long eventoIdParam) {
        return eventoRepository
                .findById(eventoIdParam)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
    }

    /**
     * Obtiene todos los eventos públicos.
     *
     * @return Lista de eventos públicos.
     */
    public List<Evento> obtenerEventosPublicos() {
        return eventoRepository.findPublicEvents();
    }

    /**
     * Obtiene todos los eventos visibles en el mapa.
     *
     * @return Lista de eventos visibles en mapa.
     */
    public List<Evento> obtenerEventosEnMapa() {
        return eventoRepository.findVisibleOnMap();
    }

    /**
     * Obtiene los eventos recomendados basados en ubicaciones populares.
     *
     * @return Lista de ubicaciones populares.
     */
    public List<String> obtenerUbicacionesRecomendadas() {
        // TODO: Implementar lógica de ubicaciones recomendadas
        return List.of();
    }

    /**
     * Genera un enlace virtual para un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @return El enlace virtual generado.
     */
    @Transactional
    public String generarEnlaceVirtual(final Long eventoIdParam) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        return evento.generarEnlaceVirtual();
    }

    // ===============================
    // EVENTOS POR COMUNIDAD
    // ===============================

    /**
     * Obtiene los eventos de una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @param incluirCancelados Si se deben incluir los eventos cancelados.
     * @return Lista de eventos de la comunidad.
     */
    public List<Evento> obtenerEventosPorComunidad(
            final Long comunidadId, final boolean incluirCancelados) {
        if (incluirCancelados) {
            return eventoRepository.findByComunidadId(comunidadId);
        }
        return eventoRepository.findByComunidadIdAndCanceladoFalse(comunidadId);
    }
}
