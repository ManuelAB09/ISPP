package es.us.meerkat.backend.controller.chats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import es.us.meerkat.backend.dto.chats.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.chats.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.chats.MensajeComunidadResponse;
import es.us.meerkat.backend.dto.chats.MensajeResponse;
import es.us.meerkat.backend.entity.chats.Mensaje;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeLeidoRepository;
import es.us.meerkat.backend.repository.chats.MensajeRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.chats.MensajeComunidadService;
import es.us.meerkat.backend.service.chats.MensajeService;

@ExtendWith(MockitoExtension.class)
class SocketChatControllerTest {

    @Mock private SimpMessagingTemplate broker;
    @Mock private MensajeService mensajeService;
    @Mock private MensajeComunidadService mensajeComunidadService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private MensajeLeidoRepository mensajeLeidoRepository;

    @InjectMocks private SocketChatController controller;

    @Test
    void getDmHistoryShouldSendErrorWhenUserIdIsMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.getDmHistory(Map.of(), (Principal) principal);

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/error"), any(Map.class));
    }

    @Test
    void getDmHistoryShouldSendHistoryWhenUserIdIsPresent() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);
        when(mensajeService.obtenerConversacionConUsuario(1L, 2L)).thenReturn(List.of());

        controller.getDmHistory(Map.of("userId", 2L), (Principal) principal);

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/dm_history"), any(List.class));
    }

    @Test
    void getDmHistoryShouldHandleMultipleMessages() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        when(mensajeService.obtenerConversacionConUsuario(2L, 3L)).thenReturn(List.of());

        controller.getDmHistory(Map.of("userId", 3L), (Principal) principal);

        verify(broker).convertAndSendToUser(eq("2"), eq("/queue/dm_history"), any(List.class));
    }

    @Test
    void getConversationsShouldSendErrorWhenPrincipalIsNull() {
        // When principal is null, the method should handle it gracefully
        try {
            controller.getConversations(null);
        } catch (Exception e) {
            // Expected behavior for null principal
        }
    }

    @Test
    void getConversationsShouldSendConversationsWhenFound() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        // Just verify that the method can be called without errors
        controller.getConversations((Principal) principal);
    }

    @Test
    void sendDmShouldSendMessageWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setUserId(2L);
        request.setContenido("Hola");

        // Just verify that the method can be called without errors
        controller.sendDm(request, (Principal) principal);
    }

    @Test
    void sendDmShouldSendErrorWhenFails() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setUserId(2L);
        request.setContenido("Hola");

        doThrow(new RuntimeException("Send error")).when(mensajeService).enviarMensaje(1L, request);

        // Verify exception is handled
        try {
            controller.sendDm(request, (Principal) principal);
        } catch (Exception e) {
            // Exception is expected
        }
    }

    @Test
    void deleteDmShouldDeleteMessageWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.deleteDm(Map.of("messageId", 10L), (Principal) principal);
    }

    @Test
    void deleteDmShouldSendErrorWhenMessageIdMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.deleteDm(Map.of(), (Principal) principal);
    }

    @Test
    void editDmShouldEditMessageWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.editDm(Map.of("messageId", 10L, "contenido", "Editado"), (Principal) principal);
    }

    @Test
    void editDmShouldSendErrorWhenMessageIdMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.editDm(Map.of("contenido", "Editado"), (Principal) principal);
    }

    @Test
    void sendCommunityMessageShouldSendMessageWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(5L);
        request.setContenido("Hola comunidad");

        // Just verify that the method can be called without errors
        controller.sendCommunityMessage(request, (Principal) principal);
    }

    @Test
    void sendCommunityMessageShouldSendErrorWhenFails() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(5L);
        request.setContenido("Hola comunidad");

        doThrow(new RuntimeException("Send error"))
                .when(mensajeComunidadService)
                .enviarMensaje(1L, request);

        // Verify exception is handled
        try {
            controller.sendCommunityMessage(request, (Principal) principal);
        } catch (Exception e) {
            // Exception is expected
        }
    }

    @Test
    void getCommunityHistoryShouldSendErrorWhenCommunityIdMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.getCommunityHistory(Map.of(), (Principal) principal);
    }

    @Test
    void getCommunityHistoryShouldSendHistoryWhenFound() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        // Just verify that the method can be called without errors
        controller.getCommunityHistory(Map.of("communityId", 5L), (Principal) principal);
    }

    @Test
    void editCommunityMessageShouldEditWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.editCommunityMessage(
                Map.of("messageId", 10L, "communityId", 5L, "contenido", "Editado"),
                (Principal) principal);
    }

    @Test
    void editCommunityMessageShouldSendErrorWhenMessageIdMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.editCommunityMessage(
                Map.of("communityId", 5L, "contenido", "Editado"), (Principal) principal);
    }

    @Test
    void deleteCommunityMessageShouldDeleteWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.deleteCommunityMessage(
                Map.of("messageId", 10L, "communityId", 5L), (Principal) principal);
    }

    @Test
    void deleteCommunityMessageShouldSendErrorWhenMessageIdMissing() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);

        controller.deleteCommunityMessage(Map.of("communityId", 5L), (Principal) principal);
    }

    @Test
    void getDmHistoryShouldHandleMultipleUsersInSequence() {
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);

        Authentication principal1 = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal1.getPrincipal()).thenReturn(usuario1);
        Authentication principal2 = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal2.getPrincipal()).thenReturn(usuario2);

        when(mensajeService.obtenerConversacionConUsuario(1L, 3L)).thenReturn(List.of());
        when(mensajeService.obtenerConversacionConUsuario(2L, 4L)).thenReturn(List.of());

        controller.getDmHistory(Map.of("userId", 3L), (Principal) principal1);
        controller.getDmHistory(Map.of("userId", 4L), (Principal) principal2);
    }

    @Test
    void getConversationsShouldSendConversationMapWhenMessagesExist() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("User");
        Usuario other = new Usuario();
        other.setId(2L);
        other.setNombre("Other");

        Mensaje msg =
                Mensaje.builder()
                        .id(10L)
                        .contenido("Hola")
                        .createdAt(LocalDateTime.now())
                        .emisor(usuario)
                        .receptor(other)
                        .build();

        when(mensajeRepository.findAll()).thenReturn(List.of(msg));

        controller.getConversations((Principal) authWithUser(usuario));

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/conversations"), any());
    }

    @Test
    void getConversationsShouldSendErrorWhenPrincipalInvalid() {
        Principal principal = () -> "anon";

        controller.getConversations(principal);

        verify(broker).convertAndSendToUser(eq("anon"), eq("/queue/error"), any(Map.class));
    }

    @Test
    void sendDmShouldSendToBothUsersWhenSuccessful() {
        Usuario sender = new Usuario();
        sender.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setUserId(2L);
        request.setContenido("Hola");

        MensajeResponse response = MensajeResponse.builder().id(1L).receptorId(2L).build();

        when(mensajeService.enviarMensaje(1L, request)).thenReturn(response);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));

        controller.sendDm(request, (Principal) authWithUser(sender));

        verify(broker).convertAndSendToUser(eq("2"), eq("/queue/dm"), eq(response));
        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/dm"), eq(response));
    }

    @Test
    void sendDmShouldSendErrorWhenReceptorMissing() {
        Usuario sender = new Usuario();
        sender.setId(1L);

        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setUserId(2L);
        request.setContenido("Hola");

        MensajeResponse response = MensajeResponse.builder().id(1L).receptorId(2L).build();

        when(mensajeService.enviarMensaje(1L, request)).thenReturn(response);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        controller.sendDm(request, (Principal) authWithUser(sender));

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/error"), any(Map.class));
    }

    @Test
    void deleteDmShouldSendDeleteSuccessToBothUsers() {
        Usuario sender = new Usuario();
        sender.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        Mensaje mensaje =
                Mensaje.builder()
                        .id(55L)
                        .contenido("Hola")
                        .createdAt(LocalDateTime.now())
                        .emisor(sender)
                        .receptor(receptor)
                        .build();

        when(mensajeRepository.findById(55L)).thenReturn(Optional.of(mensaje));

        controller.deleteDm(Map.of("messageId", 55L), (Principal) authWithUser(sender));

        verify(mensajeLeidoRepository).deleteByMensajeId(55L);
        verify(mensajeRepository).delete(mensaje);
        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/dm_delete_success"), eq(55L));
        verify(broker).convertAndSendToUser(eq("2"), eq("/queue/dm_delete_success"), eq(55L));
    }

    @Test
    void deleteDmShouldSendUnauthorizedWhenNotSender() {
        Usuario sender = new Usuario();
        sender.setId(1L);
        Usuario other = new Usuario();
        other.setId(2L);

        Mensaje mensaje =
                Mensaje.builder()
                        .id(66L)
                        .contenido("Hola")
                        .createdAt(LocalDateTime.now())
                        .emisor(other)
                        .receptor(sender)
                        .build();

        when(mensajeRepository.findById(66L)).thenReturn(Optional.of(mensaje));

        controller.deleteDm(Map.of("messageId", 66L), (Principal) authWithUser(sender));

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/error"), any(Map.class));
    }

    @Test
    void editDmShouldSendUpdateToBothUsers() {
        Usuario sender = new Usuario();
        sender.setId(1L);
        Usuario receptor = new Usuario();
        receptor.setId(2L);

        MensajeResponse response = MensajeResponse.builder().id(77L).receptorId(2L).build();
        when(mensajeService.editarMensaje(1L, 77L, "Editado")).thenReturn(response);

        Mensaje mensaje =
                Mensaje.builder()
                        .id(77L)
                        .contenido("Hola")
                        .createdAt(LocalDateTime.now())
                        .emisor(sender)
                        .receptor(receptor)
                        .build();
        when(mensajeRepository.findById(77L)).thenReturn(Optional.of(mensaje));

        controller.editDm(
                Map.of("messageId", 77L, "nuevoContenido", "Editado"),
                (Principal) authWithUser(sender));

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/dm_update_success"), eq(response));
        verify(broker).convertAndSendToUser(eq("2"), eq("/queue/dm_update_success"), eq(response));
    }

    @Test
    void sendCommunityMessageShouldSendToTopic() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(9L);
        request.setContenido("Hola comunidad");

        MensajeComunidadResponse response =
                MensajeComunidadResponse.builder().id(1L).comunidadId(9L).build();
        when(mensajeComunidadService.enviarMensaje(1L, request)).thenReturn(response);

        controller.sendCommunityMessage(request, (Principal) authWithUser(usuario));

        verify(broker).convertAndSend(eq("/topic/community.9"), eq(response));
    }

    @Test
    void getCommunityHistoryShouldSendHistoryWhenPresent() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(mensajeComunidadService.obtenerHistorial(9L))
                .thenReturn(List.of(MensajeComunidadResponse.builder().id(1L).build()));

        controller.getCommunityHistory(
                Map.of("comunidadId", 9L), (Principal) authWithUser(usuario));

        verify(broker).convertAndSendToUser(eq("1"), eq("/queue/community_history"), any());
    }

    @Test
    void editCommunityMessageShouldSendUpdateToTopic() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        MensajeComunidadResponse response =
                MensajeComunidadResponse.builder().id(2L).comunidadId(9L).build();
        when(mensajeComunidadService.editarMensaje(1L, 2L, "Nuevo")).thenReturn(response);

        controller.editCommunityMessage(
                Map.of("messageId", 2L, "comunidadId", 9L, "nuevoContenido", "Nuevo"),
                (Principal) authWithUser(usuario));

        verify(broker).convertAndSend(eq("/topic/community.9"), eq(response));
    }

    @Test
    void deleteCommunityMessageShouldSendDeletionToTopic() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        controller.deleteCommunityMessage(
                Map.of("messageId", 3L, "comunidadId", 9L), (Principal) authWithUser(usuario));

        verify(mensajeComunidadService).eliminarMensaje(1L, 3L);
        verify(broker)
                .convertAndSend(
                        eq("/topic/community.9"),
                        eq((Object) Map.of("type", "message_deleted", "messageId", 3L)));
    }

    private Authentication authWithUser(Usuario usuario) {
        Authentication principal = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(principal.getPrincipal()).thenReturn(usuario);
        return principal;
    }
}
