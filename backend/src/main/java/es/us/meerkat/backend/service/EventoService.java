package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.repository.EventoRepository;
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

    // ===============================
    // COMUNIDAD CREAR EVENTO                   //TODO: Hacer cuando la parte de comunidades este
    // ===============================

    /**
     * Crea un nuevo evento.
     *
     * @param organizadorIdParam Identificador del usuario organizador.
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaInicioParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @return El evento creado. @Transactional public Evento crearEvento(final Long
     *     organizadorIdParam, final String tituloParam, final String descripcionParam, final
     *     LocalDateTime fechaInicioParam, final LocalDateTime fechaFinParam, final Integer
     *     aforoParam, final String queLlevarParam, final Boolean esVirtualParam, final Boolean
     *     privadoParam) {
     *     <p>final Evento evento = new Evento(); evento.crear(tituloParam, descripcionParam,
     *     fechaInicioParam, fechaFinParam, ubicacionParam, latitudParam, longitudParam, aforoParam,
     *     queLlevarParam, esVirtualParam, privadoParam);
     *     <p>return eventoRepository.save(evento); }
     */
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
            final Boolean privadoParam) {

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
