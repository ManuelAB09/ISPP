package es.us.meerkat.backend.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import es.us.meerkat.backend.dto.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.MensajeComunidadResponse;
import es.us.meerkat.backend.dto.MensajeResponse;
import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.MensajeComunidadService;
import es.us.meerkat.backend.service.MensajeService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador WebSocket STOMP para manejo de mensajes privados y de eventos en
 * tiempo real.
 */
@Controller
@RequiredArgsConstructor
public class SocketChatController {

        private final SimpMessagingTemplate broker;
        private final MensajeService mensajeService;
        private final MensajeComunidadService mensajeComunidadService;
        private final UsuarioRepository usuarioRepository;
        private final MensajeRepository mensajeRepository;

        /**
         * Carga el historial de conversaciones privadas del usuario autenticado.
         *
         * @param principal usuario autenticado.
         */
        @MessageMapping("/conversations.get")
        public void getConversations(final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long userId = usuario.getId();

                        final List<Mensaje> messages = mensajeRepository.findAll();
                        final Map<Long, Map<String, Object>> conversationMap = new HashMap<>();

                        for (final Mensaje msg : messages) {
                                if (msg.getEmisor().getId().equals(userId)
                                                || msg.getReceptor().getId().equals(userId)) {
                                        final Long otherId = msg.getEmisor().getId().equals(userId)
                                                        ? msg.getReceptor().getId()
                                                        : msg.getEmisor().getId();

                                        if (!conversationMap.containsKey(otherId)) {
                                                final Usuario otherUser = msg.getEmisor().getId().equals(userId)
                                                                ? msg.getReceptor()
                                                                : msg.getEmisor();
                                                conversationMap.put(
                                                                otherId,
                                                                new HashMap<>(
                                                                                Map.of(
                                                                                                "userId", otherId,
                                                                                                "userName",
                                                                                                otherUser.getNombre(),
                                                                                                "lastMessage",
                                                                                                msg.getContenido(),
                                                                                                "lastMessageTime",
                                                                                                msg.getCreatedAt())));
                                        }
                                }
                        }

                        broker.convertAndSendToUser(
                                        getUserDestinationKey(usuario),
                                        "/queue/conversations",
                                        conversationMap.values());
                } catch (final Exception e) {
                        sendError(principal, "conversations_error", e.getMessage());
                }
        }

        /**
         * Envía un mensaje privado entre dos usuarios.
         *
         * @param request   datos del mensaje (tutorId, contenido).
         * @param principal usuario autenticado.
         */
        @MessageMapping("/dm.send")
        public void sendDm(@Payload final EnviarMensajeRequest request, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);

                        final MensajeResponse response = mensajeService.enviarMensaje(usuario.getId(), request);
                        final Usuario receptor = usuarioRepository
                                        .findById(response.getReceptorId())
                                        .orElseThrow(() -> new RuntimeException("Receptor no encontrado"));

                        broker.convertAndSendToUser(getUserDestinationKey(receptor), "/queue/dm", response);
                        broker.convertAndSendToUser(getUserDestinationKey(usuario), "/queue/dm", response);
                } catch (final Exception e) {
                        sendError(principal, "dm_send_failed", e.getMessage());
                }
        }

        /**
         * Carga el historial de mensajes privados con un usuario específico.
         *
         * @param payload   map con el userId del otro usuario.
         * @param principal usuario autenticado.
         */
        @MessageMapping("/dm.history")
        public void getDmHistory(@Payload final Map<String, Long> payload, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long otherUserId = payload.get("userId");

                        if (otherUserId == null) {
                                broker.convertAndSendToUser(
                                                getUserDestinationKey(usuario),
                                                "/queue/error",
                                                Map.of("code", "dm_history_missing", "message", "Missing userId"));
                                return;
                        }

                        final List<MensajeResponse> history = mensajeService
                                        .obtenerConversacionConUsuario(usuario.getId(), otherUserId);

                        broker.convertAndSendToUser(
                                        getUserDestinationKey(usuario), "/queue/dm_history", history);
                } catch (final Exception e) {
                        sendError(principal, "dm_history_failed", e.getMessage());
                }
        }

        /**
         * Elimina un mensaje privado (solo el emisor puede hacerlo).
         *
         * @param payload   map con messageId.
         * @param principal usuario autenticado.
         */
        @MessageMapping("/dm.delete")
        public void deleteDm(@Payload final Map<String, Long> payload, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long messageId = payload.get("messageId");

                        if (messageId == null) {
                                broker.convertAndSendToUser(
                                                getUserDestinationKey(usuario),
                                                "/queue/error",
                                                Map.of("code", "delete_missing", "message", "Missing messageId"));
                                return;
                        }

                        final Mensaje mensaje = mensajeRepository
                                        .findById(messageId)
                                        .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

                        if (!mensaje.getEmisor().getId().equals(usuario.getId())) {
                                broker.convertAndSendToUser(
                                                getUserDestinationKey(usuario),
                                                "/queue/error",
                                                Map.of("code", "unauthorized_delete", "message", "No tienes permiso"));
                                return;
                        }

                        mensajeRepository.delete(mensaje);
                        final Usuario receptor = mensaje.getReceptor();

                        // Notificar a ambos usuarios de la eliminación
                        broker.convertAndSendToUser(
                                        getUserDestinationKey(usuario), "/queue/dm_delete_success", messageId);
                        broker.convertAndSendToUser(
                                        getUserDestinationKey(receptor), "/queue/dm_delete_success", messageId);
                } catch (final Exception e) {
                        sendError(principal, "delete_dm_failed", e.getMessage());
                }
        }

        /**
         * Envía un mensaje en el chat de una comunidad.
         *
         * @param request   datos del mensaje (comunidadId, contenido).
         * @param principal usuario autenticado.
         */
        @MessageMapping("/community.message.send")
        public void sendCommunityMessage(
                        @Payload final EnviarMensajeComunidadRequest request, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);

                        final MensajeComunidadResponse response = mensajeComunidadService.enviarMensaje(usuario.getId(),
                                        request);

                        broker.convertAndSend("/topic/community." + request.getComunidadId(), response);
                } catch (final Exception e) {
                        sendError(principal, "community_message_failed", e.getMessage());
                }
        }

        /**
         * Obtiene el historial de mensajes de una comunidad.
         *
         * @param payload   map con comunidadId.
         * @param principal usuario autenticado.
         */
        @MessageMapping("/community.history")
        public void getCommunityHistory(
                        @Payload final Map<String, Long> payload, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long comunidadId = payload.get("comunidadId");

                        if (comunidadId == null) {
                                broker.convertAndSendToUser(
                                                getUserDestinationKey(usuario),
                                                "/queue/error",
                                                Map.of("code", "community_missing", "message", "Missing comunidadId"));
                                return;
                        }

                        final java.util.List<MensajeComunidadResponse> history = mensajeComunidadService
                                        .obtenerHistorial(comunidadId);

                        broker.convertAndSendToUser(
                                        getUserDestinationKey(usuario), "/queue/community_history", history);
                } catch (final Exception e) {
                        sendError(principal, "community_history_failed", e.getMessage());
                }
        }

        /**
         * Edita un mensaje de comunidad (solo el autor puede hacerlo).
         *
         * @param payload   map con messageId, comunidadId y nuevoContenido.
         * @param principal usuario autenticado.
         */
        @MessageMapping("/community.message.edit")
        public void editCommunityMessage(
                        @Payload final Map<String, Object> payload, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long messageId = Long.parseLong(payload.get("messageId").toString());
                        final Long comunidadId = Long.parseLong(payload.get("comunidadId").toString());
                        final String nuevoContenido = payload.get("nuevoContenido").toString();

                        final MensajeComunidadResponse response = mensajeComunidadService.editarMensaje(
                                        usuario.getId(), messageId, nuevoContenido);

                        broker.convertAndSend("/topic/community." + comunidadId, response);
                } catch (final Exception e) {
                        sendError(principal, "community_edit_failed", e.getMessage());
                }
        }

        /**
         * Elimina un mensaje de comunidad (solo el autor puede hacerlo).
         *
         * @param payload   map con messageId y comunidadId.
         * @param principal usuario autenticado.
         */
        @MessageMapping("/community.message.delete")
        public void deleteCommunityMessage(
                        @Payload final Map<String, Long> payload, final Principal principal) {
                try {
                        final Usuario usuario = getAuthenticatedUser(principal);
                        final Long messageId = payload.get("messageId");
                        final Long comunidadId = payload.get("comunidadId");

                        if (messageId == null || comunidadId == null) {
                                broker.convertAndSendToUser(
                                                getUserDestinationKey(usuario),
                                                "/queue/error",
                                                Map.of(
                                                                "code",
                                                                "community_delete_missing",
                                                                "message",
                                                                "Missing messageId or comunidadId"));
                                return;
                        }

                        mensajeComunidadService.eliminarMensaje(usuario.getId(), messageId);

                        broker.convertAndSend(
                                        "/topic/community." + comunidadId,
                                        (Object) Map.of("type", "message_deleted", "messageId", messageId));
                } catch (final Exception e) {
                        sendError(principal, "community_delete_failed", e.getMessage());
                }
        }

        private Usuario getAuthenticatedUser(final Principal principal) {
                if (!(principal instanceof org.springframework.security.core.Authentication authentication)
                                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
                        throw new RuntimeException("Usuario no autenticado en WebSocket");
                }
                return usuario;
        }

        private String getUserDestinationKey(final Usuario usuario) {
                return usuario.getEmail();
        }

        private void sendError(final Principal principal, final String code, final String message) {
                final Map<String, String> payload = Map.of("code", code, "message", message);

                if (principal instanceof org.springframework.security.core.Authentication authentication
                                && authentication.getPrincipal() instanceof Usuario usuario) {
                        broker.convertAndSendToUser(getUserDestinationKey(usuario), "/queue/error", payload);
                        return;
                }

                if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
                        broker.convertAndSendToUser(principal.getName(), "/queue/error", payload);
                        return;
                }

                broker.convertAndSend("/topic/errors", payload);
        }
}
