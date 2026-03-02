package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.MensajeResponse;
import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensajeService {
    /** Repositorio para acceder a la información de tutores. */
    private final TutorRepository tutorRepository;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    private final MensajeRepository mensajeRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureTutorColumnNullable() {
        try {
            jdbcTemplate.execute("ALTER TABLE mensaje ALTER COLUMN tutor_id DROP NOT NULL");
        } catch (Exception ignored) {
            try {
                jdbcTemplate.execute("ALTER TABLE MENSAJE ALTER COLUMN TUTOR_ID DROP NOT NULL");
            } catch (Exception ignoredAgain) {
                // Ignorado: en algunos entornos ya está nullable o la sintaxis difiere.
            }
        }
    }

    @Transactional
    public MensajeResponse enviarMensaje(Long usuarioId, EnviarMensajeRequest request) {
        if (request == null || request.getContenido() == null || request.getContenido().isBlank()) {
            throw new IllegalArgumentException("El contenido del mensaje es obligatorio");
        }

        Usuario emisor =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario receptor;
        Tutor tutor = null;

        if (request.getUserId() != null) {
            receptor =
                    usuarioRepository
                            .findById(request.getUserId())
                            .orElseThrow(
                                    () -> new RuntimeException("Usuario receptor no encontrado"));
        } else if (request.getTutorId() != null) {
            tutor =
                    tutorRepository
                            .findById(request.getTutorId())
                            .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

            if (!Boolean.TRUE.equals(tutor.getVerificado())) {
                throw new RuntimeException("No puedes contactar un tutor no verificado");
            }

            receptor = tutor.getUs();
        } else {
            throw new IllegalArgumentException("Debes indicar userId o tutorId");
        }

        if (receptor.getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No puedes enviarte mensajes a ti mismo");
        }

        Mensaje mensaje =
                Mensaje.builder()
                        .contenido(request.getContenido())
                        .emisor(emisor)
                        .receptor(receptor)
                        .tutor(tutor)
                        .build();

        mensajeRepository.save(mensaje);

        return mapToResponse(mensaje);
    }

    @Transactional(readOnly = true)
    public List<MensajeResponse> obtenerConversacion(Long usuarioId, Long tutorId) {

        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        Long tutorUserId = tutor.getUs().getId();

        List<Mensaje> mensajes =
                mensajeRepository.findConversationWithTutor(tutorId, usuarioId, tutorUserId);

        return mensajes.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeResponse> obtenerConversacionConUsuario(Long usuarioId, Long otherUserId) {
        if (otherUserId == null) {
            throw new IllegalArgumentException("Usuario destino no indicado");
        }

        usuarioRepository
                .findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Mensaje> mensajes =
                mensajeRepository
                        .findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByCreatedAtAsc(
                                usuarioId, otherUserId, otherUserId, usuarioId);

        return mensajes.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void eliminarMensaje(Long usuarioId, Long mensajeId) {
        Mensaje mensaje =
                mensajeRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));

        if (!mensaje.getEmisor().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar este mensaje");
        }

        mensajeRepository.delete(mensaje);
    }

    private MensajeResponse mapToResponse(Mensaje mensaje) {
        return MensajeResponse.builder()
                .id(mensaje.getId())
                .contenido(mensaje.getContenido())
                .editado(mensaje.getEditado())
                .createdAt(mensaje.getCreatedAt())
                .emisorId(mensaje.getEmisor().getId())
                .receptorId(mensaje.getReceptor().getId())
                .build();
    }
}
