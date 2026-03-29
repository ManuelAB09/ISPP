package es.us.meerkat.backend.service.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import es.us.meerkat.backend.entity.notifications.Notificacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.notifications.NotificacionRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificacionService notificacionService;

    // ================================================================
    // obtenerNotificaciones
    // ================================================================

    @Test
    void obtenerNotificacionesShouldReturnUserNotifications() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("U").email("u@t.com").password("p").build();
        Notificacion n1 =
                Notificacion.builder()
                        .id(1L)
                        .titulo("T1")
                        .mensaje("M1")
                        .tipo("ANUNCIO")
                        .usuario(usuario)
                        .build();
        Notificacion n2 =
                Notificacion.builder()
                        .id(2L)
                        .titulo("T2")
                        .mensaje("M2")
                        .tipo("EVENTO")
                        .usuario(usuario)
                        .build();

        when(notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario))
                .thenReturn(List.of(n1, n2));

        List<Notificacion> result = notificacionService.obtenerNotificaciones(usuario);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitulo()).isEqualTo("T1");
    }

    @Test
    void obtenerNotificacionesShouldReturnEmptyListWhenNoNotifications() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("U").email("u@t.com").password("p").build();
        when(notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario))
                .thenReturn(List.of());

        assertThat(notificacionService.obtenerNotificaciones(usuario)).isEmpty();
    }

    // ================================================================
    // marcarComoLeida
    // ================================================================

    @Test
    void marcarComoLeidaShouldReturnTrueAndMarkAsRead() {
        Notificacion notificacion =
                Notificacion.builder()
                        .id(1L)
                        .titulo("T")
                        .mensaje("M")
                        .tipo("ANUNCIO")
                        .leida(false)
                        .usuario(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("U")
                                        .email("u@t.com")
                                        .password("p")
                                        .build())
                        .build();

        when(notificacionRepository.findByIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        boolean result = notificacionService.marcarComoLeida(1L, 1L);

        assertThat(result).isTrue();
        assertThat(notificacion.getLeida()).isTrue();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarComoLeidaShouldReturnFalseWhenNotFound() {
        when(notificacionRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        boolean result = notificacionService.marcarComoLeida(99L, 1L);

        assertThat(result).isFalse();
    }

    @Test
    void marcarComoLeidaShouldReturnFalseWhenNotificationBelongsToDifferentUser() {
        when(notificacionRepository.findByIdAndUsuarioId(1L, 99L)).thenReturn(Optional.empty());

        boolean result = notificacionService.marcarComoLeida(1L, 99L);

        assertThat(result).isFalse();
    }

    // ================================================================
    // crearYNotificar
    // ================================================================

    @Test
    void crearYNotificarShouldSaveAndSendWebSocketWhenPushEnabled() {
        Usuario usuario =
                Usuario.builder()
                        .id(5L)
                        .nombre("U")
                        .email("u@t.com")
                        .password("p")
                        .notificacionesPush(true)
                        .build();
        Notificacion notificacion =
                Notificacion.builder()
                        .titulo("Nuevo Anuncio")
                        .mensaje("Msg")
                        .tipo("ANUNCIO")
                        .usuario(usuario)
                        .build();

        when(notificacionRepository.save(notificacion)).thenReturn(notificacion);

        Notificacion result = notificacionService.crearYNotificar(notificacion);

        assertThat(result).isEqualTo(notificacion);
        verify(messagingTemplate).convertAndSendToUser("5", "/queue/notificaciones", notificacion);
    }

    @Test
    void crearYNotificarShouldSaveAndSendWebSocketWhenPushIsNull() {
        Usuario usuario =
                Usuario.builder()
                        .id(5L)
                        .nombre("U")
                        .email("u@t.com")
                        .password("p")
                        .notificacionesPush(null)
                        .build();
        Notificacion notificacion =
                Notificacion.builder()
                        .titulo("T")
                        .mensaje("M")
                        .tipo("EVENTO")
                        .usuario(usuario)
                        .build();

        when(notificacionRepository.save(notificacion)).thenReturn(notificacion);

        notificacionService.crearYNotificar(notificacion);

        verify(messagingTemplate).convertAndSendToUser("5", "/queue/notificaciones", notificacion);
    }

    @Test
    void crearYNotificarShouldSaveButNotSendWebSocketWhenPushDisabled() {
        Usuario usuario =
                Usuario.builder()
                        .id(5L)
                        .nombre("U")
                        .email("u@t.com")
                        .password("p")
                        .notificacionesPush(false)
                        .build();
        Notificacion notificacion =
                Notificacion.builder()
                        .titulo("T")
                        .mensaje("M")
                        .tipo("ANUNCIO")
                        .usuario(usuario)
                        .build();

        when(notificacionRepository.save(notificacion)).thenReturn(notificacion);

        Notificacion result = notificacionService.crearYNotificar(notificacion);

        assertThat(result).isEqualTo(notificacion);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}
