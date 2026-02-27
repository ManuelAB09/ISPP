package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio relacionada con la asistencia a eventos.
 *
 * <p>Incluye confirmación de asistencia, cancelación y obtención de información de asistencia.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaEventoService {

    /** Repositorio para acceder a la información de asistencia a eventos. */
    private final AsistenciaEventoRepository asistenciaRepository;

    /** Repositorio para acceder a la información de eventos. */
    private final EventoRepository eventoRepository;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Repositorio para acceder a la información de miembros de comunidad. */
    private final MiembroComunidadRepository miembroRepository;

    // ===============================
    // CONFIRMAR ASISTENCIA
    // ===============================

    /**
     * Confirma la asistencia de un usuario a un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @param usuarioIdParam Identificador del usuario.
     * @return La asistencia confirmada.
     */
    @Transactional
    public AsistenciaEvento confirmarAsistencia(
            final Long eventoIdParam, final Long usuarioIdParam) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioIdParam)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si el usuario es miembro de la comunidad del evento
        if (evento.getComunidad() != null) {
            miembroRepository.findByUsuarioIdAndComunidadId(
                    usuarioIdParam, evento.getComunidad().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Debes ser miembro de la comunidad para apuntarte a este evento"));
        }

        // Verificar si el evento está lleno
        if (evento.verificarAforo()) {
            throw new RuntimeException("El evento ha alcanzado su aforo máximo");
        }

        // Verificar si ya existe una asistencia
        final var existente =
                asistenciaRepository.findByEventoAndUsuario(eventoIdParam, usuarioIdParam);
        if (existente.isPresent()) {
            final AsistenciaEvento asistencia = existente.get();
            final boolean yaConfirmada = EstadoAsistencia.CONFIRMADA.equals(asistencia.getEstado());
            asistencia.confirmarAsistencia();
            if (!yaConfirmada) {
                evento.setAsistentesConfirmados(evento.contarAsistentes() + 1);
                eventoRepository.save(evento);
            }
            return asistenciaRepository.save(asistencia);
        }

        // Crear nueva asistencia
        final AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setUsuario(usuario);
        asistencia.confirmarAsistencia();
        asistencia.setCreatedAt(LocalDateTime.now());

        // Actualizar contador de asistentes en el evento
        evento.setAsistentesConfirmados(evento.contarAsistentes() + 1);
        eventoRepository.save(evento);

        return asistenciaRepository.save(asistencia);
    }

    // ===============================
    // CANCELAR ASISTENCIA
    // ===============================

    /**
     * Cancela la asistencia de un usuario a un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @param usuarioIdParam Identificador del usuario.
     */
    @Transactional
    public void cancelarAsistencia(final Long eventoIdParam, final Long usuarioIdParam) {
        final AsistenciaEvento asistencia =
                asistenciaRepository
                        .findByEventoAndUsuario(eventoIdParam, usuarioIdParam)
                        .orElseThrow(() -> new RuntimeException("Asistencia no encontrada"));

        final boolean estabaConfirmada = EstadoAsistencia.CONFIRMADA.equals(asistencia.getEstado());
        asistencia.cancelarAsistencia();
        asistenciaRepository.save(asistencia);

        // Actualizar contador de asistentes en el evento
        if (estabaConfirmada) {
            final Evento evento =
                    eventoRepository
                            .findById(eventoIdParam)
                            .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
            evento.setAsistentesConfirmados(Math.max(evento.contarAsistentes() - 1, 0));
            eventoRepository.save(evento);
        }
    }

    // ===============================
    // OBTENER ASISTENCIA
    // ===============================

    /**
     * Obtiene la asistencia de un usuario a un evento específico.
     *
     * @param eventoIdParam Identificador del evento.
     * @param usuarioIdParam Identificador del usuario.
     * @return La asistencia del usuario.
     */
    public AsistenciaEvento obtenerAsistencia(final Long eventoIdParam, final Long usuarioIdParam) {
        return asistenciaRepository
                .findByEventoAndUsuario(eventoIdParam, usuarioIdParam)
                .orElseThrow(() -> new RuntimeException("Asistencia no encontrada"));
    }

    /**
     * Obtiene todas las asistencias confirmadas a un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @return Lista de asistencias confirmadas.
     */
    public List<AsistenciaEvento> obtenerAsistentesConfirmados(final Long eventoIdParam) {
        return asistenciaRepository.findConfirmedAttendanceByEvent(eventoIdParam);
    }

    /**
     * Obtiene todas las asistencias de un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @return Lista de todas las asistencias del evento.
     */
    public List<AsistenciaEvento> obtenerAsistenciasEvento(final Long eventoIdParam) {
        return asistenciaRepository.findByEventoId(eventoIdParam);
    }

    /**
     * Obtiene el número total de asistentes confirmados a un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @return Número de asistentes confirmados.
     */
    public long contarAsistentesConfirmados(final Long eventoIdParam) {
        return asistenciaRepository.countConfirmedByEvent(eventoIdParam);
    }
}
