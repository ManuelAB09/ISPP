package es.us.meerkat.backend.service.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.chats.MensajeComunidad;
import es.us.meerkat.backend.entity.chats.MensajeComunidadLeido;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeComunidadLeidoRepository;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadLeidoServiceTest {

    @Mock private MensajeComunidadLeidoRepository mensajeComunidadLeidoRepository;

    @InjectMocks private MensajeComunidadLeidoService mensajeComunidadLeidoService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).nombre("User").email("u@t.com").password("p").build();
    }

    @Test
    void marcarComoLeidoShouldSaveWhenNotAlreadyRead() {
        MensajeComunidad mensaje = MensajeComunidad.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario))
                .thenReturn(Optional.empty());

        mensajeComunidadLeidoService.marcarComoLeido(mensaje, usuario);

        verify(mensajeComunidadLeidoRepository).save(any(MensajeComunidadLeido.class));
    }

    @Test
    void marcarComoLeidoShouldNotSaveWhenAlreadyRead() {
        MensajeComunidad mensaje = MensajeComunidad.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario))
                .thenReturn(Optional.of(new MensajeComunidadLeido()));

        mensajeComunidadLeidoService.marcarComoLeido(mensaje, usuario);

        verify(mensajeComunidadLeidoRepository, never()).save(any());
    }

    @Test
    void estaLeidoShouldReturnTrueWhenRead() {
        MensajeComunidad mensaje = MensajeComunidad.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario))
                .thenReturn(Optional.of(new MensajeComunidadLeido()));

        assertThat(mensajeComunidadLeidoService.estaLeido(mensaje, usuario)).isTrue();
    }

    @Test
    void estaLeidoShouldReturnFalseWhenNotRead() {
        MensajeComunidad mensaje = MensajeComunidad.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario))
                .thenReturn(Optional.empty());

        assertThat(mensajeComunidadLeidoService.estaLeido(mensaje, usuario)).isFalse();
    }
}
