package es.us.meerkat.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import es.us.meerkat.backend.controller.chats.SocketChatController;
import es.us.meerkat.backend.entity.users.Usuario;
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
}
