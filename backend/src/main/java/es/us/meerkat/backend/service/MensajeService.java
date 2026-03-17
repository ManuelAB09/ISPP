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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
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

            receptor = tutor.getUsuario();
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

    @Transactional
    public MensajeResponse enviarArchivo(
            Long usuarioId,
            Long userId,
            Long tutorId,
            String contenido,
            String archivoNombre,
            String archivoMimeType,
            Long archivoTamano,
            byte[] archivoData) {
        if (archivoData == null || archivoData.length == 0) {
            throw new IllegalArgumentException("El contenido del archivo es obligatorio");
        }

        Usuario emisor =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario receptor;
        Tutor tutor = null;

        if (userId != null) {
            receptor =
                    usuarioRepository
                            .findById(userId)
                            .orElseThrow(
                                    () -> new RuntimeException("Usuario receptor no encontrado"));
        } else if (tutorId != null) {
            tutor =
                    tutorRepository
                            .findById(tutorId)
                            .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

            if (!Boolean.TRUE.equals(tutor.getVerificado())) {
                throw new RuntimeException("No puedes contactar un tutor no verificado");
            }

            receptor = tutor.getUsuario();
        } else {
            throw new IllegalArgumentException("Debes indicar userId o tutorId");
        }

        if (receptor.getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No puedes enviarte mensajes a ti mismo");
        }

        String contenidoFinal =
                (contenido == null || contenido.isBlank())
                        ? "[Adjunto] " + archivoNombre
                        : contenido;

        Mensaje mensaje =
                Mensaje.builder()
                        .contenido(contenidoFinal)
                        .archivoNombre(archivoNombre)
                        .archivoMimeType(archivoMimeType)
                        .archivoTamano(archivoTamano)
                        .archivoData(archivoData)
                        .emisor(emisor)
                        .receptor(receptor)
                        .tutor(tutor)
                        .build();

        mensajeRepository.save(mensaje);
        mensaje.setArchivoUrl("/api/v1/mensajes/" + mensaje.getId() + "/archivo");
        mensajeRepository.save(mensaje);
        return mapToResponse(mensaje);
    }

    @Transactional(readOnly = true)
    public MensajeArchivo obtenerArchivo(final Long usuarioId, final Long mensajeId) {
        final Mensaje mensaje =
                mensajeRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));

        final boolean autorizado =
                mensaje.getEmisor().getId().equals(usuarioId)
                        || mensaje.getReceptor().getId().equals(usuarioId);
        if (!autorizado) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este archivo");
        }

        if (mensaje.getArchivoData() == null || mensaje.getArchivoData().length == 0) {
            throw new IllegalArgumentException("El mensaje no contiene archivo");
        }

        return new MensajeArchivo(
                mensaje.getArchivoData(), mensaje.getArchivoNombre(), mensaje.getArchivoMimeType());
    }

    @Transactional(readOnly = true)
    public List<MensajeResponse> obtenerConversacion(Long usuarioId, Long tutorId) {

        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        Long tutorUserId = tutor.getUsuario().getId();

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

    @Transactional(readOnly = true)
    public List<?> obtenerConversaciones(Long usuarioId) {
        log.info("Obteniendo conversaciones para usuario: {}", usuarioId);

        usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Mensaje> messages = mensajeRepository.findAllConversations(usuarioId);
        log.info("Total de mensajes encontrados: {}", messages.size());

        java.util.Map<Long, java.util.Map<String, Object>> conversationMap =
                new java.util.LinkedHashMap<>();

        for (Mensaje msg : messages) {
            try {
                // Validar que emisor y receptor no sean nulos
                if (msg.getEmisor() == null || msg.getReceptor() == null) {
                    log.warn("Mensaje {} tiene emisor o receptor nulo, omitiendo", msg.getId());
                    continue;
                }

                Long otherId =
                        msg.getEmisor().getId().equals(usuarioId)
                                ? msg.getReceptor().getId()
                                : msg.getEmisor().getId();
                Usuario otherUser =
                        msg.getEmisor().getId().equals(usuarioId)
                                ? msg.getReceptor()
                                : msg.getEmisor();

                if (!conversationMap.containsKey(otherId)) {
                    java.util.Map<String, Object> convData = new java.util.HashMap<>();
                    convData.put("usuarioId", otherId);
                    convData.put("usuarioNombre", otherUser.getNombre());
                    convData.put("usuarioFoto", otherUser.getFoto());
                    convData.put("ultimoMensaje", msg.getContenido());
                    convData.put("ultimaFecha", msg.getCreatedAt());
                    conversationMap.put(otherId, convData);
                }
            } catch (Exception e) {
                log.error("Error procesando mensaje {}: {}", msg.getId(), e.getMessage(), e);
            }
        }

        log.info("Total de conversaciones únicas: {}", conversationMap.size());
        return new java.util.ArrayList<>(conversationMap.values());
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

    @Transactional
    public MensajeResponse editarMensaje(Long usuarioId, Long mensajeId, String nuevoContenido) {
        if (nuevoContenido == null || nuevoContenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío");
        }

        Mensaje mensaje =
                mensajeRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));

        if (!mensaje.getEmisor().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para editar este mensaje");
        }

        mensaje.setContenido(nuevoContenido.trim());
        mensaje.setEditado(true);
        mensajeRepository.save(mensaje);

        return mapToResponse(mensaje);
    }

    private MensajeResponse mapToResponse(Mensaje mensaje) {
        return MensajeResponse.builder()
                .id(mensaje.getId())
                .contenido(mensaje.getContenido())
                .editado(mensaje.getEditado())
                .createdAt(mensaje.getCreatedAt())
                .emisorId(mensaje.getEmisor().getId())
                .receptorId(mensaje.getReceptor().getId())
                .archivoUrl(mensaje.getArchivoUrl())
                .archivoNombre(mensaje.getArchivoNombre())
                .archivoMimeType(mensaje.getArchivoMimeType())
                .archivoTamano(mensaje.getArchivoTamano())
                .build();
    }

    /** Obtiene un mensaje por su ID y lo devuelve como DTO. */
    @Transactional(readOnly = true)
    public MensajeResponse obtenerMensaje(Long mensajeId) {
        Mensaje mensaje =
                mensajeRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));
        return mapToResponse(mensaje);
    }

    public record MensajeArchivo(byte[] data, String nombre, String mimeType) {}
}
