package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /** Servicio de autorización para verificar roles en comunidades. */
    private final AuthorizationService authorizationService;

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

        validarAforo(aforoParam);
        validarFechaInicioNoPasada(fechaHoraParam);

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
            final Ubicacion ubicacion =
                    ubicacionRepository
                            .findById(ubicacionId)
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
            final Long ubicacionId,
            final Boolean visibleMapaParam) {

        validarAforo(aforoParam);
        validarFechaInicioNoPasada(fechaInicioParam);

        // Validar que la fecha de inicio sea anterior a la fecha de fin
        if (fechaFinParam != null && fechaInicioParam.isAfter(fechaFinParam)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        validarEventoNoIniciado(evento);

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
            final Ubicacion ubicacion =
                    ubicacionRepository
                            .findById(ubicacionId)
                            .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
            evento.setUbicacion(ubicacion);
        } else if (Boolean.TRUE.equals(esVirtualParam)) {
            evento.setUbicacion(null);
        }

        if (visibleMapaParam != null) {
            evento.setVisibleMapa(visibleMapaParam);
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

        validarEventoNoIniciado(evento);

        evento.cancelar(motivoParam);
        return eventoRepository.save(evento);
    }

    private void validarEventoNoIniciado(final Evento evento) {
        if (evento.getFechaHora() != null && !evento.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "No se puede modificar o cancelar un evento que ya ha comenzado");
        }
    }

    private void validarAforo(final Integer aforo) {
        if (aforo == null || aforo < 1 || aforo > 999) {
            throw new IllegalArgumentException("El aforo debe ser un numero entre 1 y 999");
        }
    }

    private void validarFechaInicioNoPasada(final LocalDateTime fechaInicio) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }

        if (fechaInicio.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La fecha y hora de inicio no puede ser anterior a la fecha y hora actual");
        }
    }

    // ===============================
    // OBTENER EVENTO
    // ===============================

    /**
     * Obtiene un evento por su ID, verificando permisos de visibilidad.
     *
     * @param eventoIdParam Identificador del evento.
     * @param usuarioId Identificador del usuario que solicita (puede ser null).
     * @return El evento encontrado.
     */
    public Evento obtenerEvento(final Long eventoIdParam, final Long usuarioId) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if (Boolean.TRUE.equals(evento.getPrivado())) {
            if (!puedeVerEventoPrivado(evento, usuarioId)) {
                throw new RuntimeException("No tienes permiso para ver este evento privado");
            }
        }

        return evento;
    }

    /**
     * Obtiene un evento por su ID sin verificar visibilidad (uso interno).
     *
     * @param eventoIdParam Identificador del evento.
     * @return El evento encontrado.
     */
    public Evento obtenerEventoInterno(final Long eventoIdParam) {
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
     * Obtiene todos los eventos visibles en el mapa (públicos, no cancelados). Opcionalmente filtra
     * por radio de distancia respecto a una ubicación.
     *
     * @param lat Latitud del centro de búsqueda (opcional).
     * @param lon Longitud del centro de búsqueda (opcional).
     * @param radioKm Radio de búsqueda en kilómetros (opcional).
     * @return Lista de eventos visibles en mapa.
     */
    public List<Evento> obtenerEventosEnMapa(
            final Double lat, final Double lon, final Double radioKm) {
        List<Evento> eventos = eventoRepository.findVisibleOnMap();
        if (lat == null || lon == null || radioKm == null) {
            return eventos;
        }
        return eventos.stream()
                .filter(
                        evento -> {
                            Ubicacion ubicacion = evento.getUbicacion();
                            if (ubicacion == null
                                    || ubicacion.getLatitud() == null
                                    || ubicacion.getLongitud() == null) {
                                return false;
                            }
                            double distancia =
                                    calcularDistanciaKm(
                                            lat,
                                            lon,
                                            ubicacion.getLatitud(),
                                            ubicacion.getLongitud());
                            return distancia <= radioKm;
                        })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los nombres de ubicaciones con eventos activos dentro de un radio dado.
     *
     * @param lat Latitud del centro de búsqueda.
     * @param lon Longitud del centro de búsqueda.
     * @param radioKm Radio de búsqueda en kilómetros.
     * @return Lista de nombres de ubicaciones recomendadas.
     */
    @Transactional(readOnly = true)
    public List<String> obtenerUbicacionesRecomendadas(
            final Double lat, final Double lon, final Double radioKm) {
        if (lat == null || lon == null || radioKm == null) {
            return List.of();
        }
        List<Evento> eventos = eventoRepository.findVisibleOnMap();
        return eventos.stream()
                .filter(
                        e ->
                                e.getUbicacion() != null
                                        && e.getUbicacion().getLatitud() != null
                                        && e.getUbicacion().getLongitud() != null)
                .filter(
                        e ->
                                calcularDistanciaKm(
                                                lat,
                                                lon,
                                                e.getUbicacion().getLatitud(),
                                                e.getUbicacion().getLongitud())
                                        <= radioKm)
                .map(e -> e.getUbicacion().getNombre())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calcula la distancia en kilómetros entre dos puntos geográficos usando la fórmula de
     * Haversine.
     */
    private double calcularDistanciaKm(
            final double lat1, final double lon1, final double lat2, final double lon2) {
        final double radioTierra = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radioTierra * c;
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
     * Obtiene los eventos de una comunidad, filtrando eventos privados según permisos.
     *
     * @param comunidadId Identificador de la comunidad.
     * @param incluirCancelados Si se deben incluir los eventos cancelados.
     * @param usuarioId Identificador del usuario que solicita (puede ser null).
     * @return Lista de eventos de la comunidad visibles para el usuario.
     */
    public List<Evento> obtenerEventosPorComunidad(
            final Long comunidadId, final boolean incluirCancelados, final Long usuarioId) {
        List<Evento> eventos;
        if (incluirCancelados) {
            eventos = eventoRepository.findByComunidadId(comunidadId);
        } else {
            eventos = eventoRepository.findByComunidadIdAndCanceladoFalse(comunidadId);
        }
        return filtrarEventosPrivados(eventos, usuarioId);
    }

    /**
     * Verifica si un usuario puede ver un evento privado. Solo los miembros de la comunidad pueden
     * verlo.
     */
    private boolean puedeVerEventoPrivado(final Evento evento, final Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        if (evento.getComunidad() != null) {
            return authorizationService.isMemberOf(usuarioId, evento.getComunidad().getId());
        }
        return false;
    }

    /** Filtra una lista de eventos eliminando los privados que el usuario no puede ver. */
    private List<Evento> filtrarEventosPrivados(final List<Evento> eventos, final Long usuarioId) {
        return eventos.stream()
                .filter(
                        evento ->
                                !Boolean.TRUE.equals(evento.getPrivado())
                                        || puedeVerEventoPrivado(evento, usuarioId))
                .collect(Collectors.toList());
    }
}
