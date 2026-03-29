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

import es.us.meerkat.backend.entity.chats.Mensaje;
import es.us.meerkat.backend.entity.chats.MensajeLeido;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeLeidoRepository;

@ExtendWith(MockitoExtension.class)
class MensajeLeidoServiceTest {

    @Mock private MensajeLeidoRepository mensajeLeidoRepository;

    @InjectMocks private MensajeLeidoService mensajeLeidoService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder().id(id).nombre("User").email("u@t.com").password("p").build();
    }

    @Test
    void marcarComoLeidoShouldSaveWhenNotAlreadyRead() {
        Mensaje mensaje = Mensaje.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario))
                .thenReturn(Optional.empty());

        mensajeLeidoService.marcarComoLeido(mensaje, usuario);

        verify(mensajeLeidoRepository).save(any(MensajeLeido.class));
    }

    @Test
    void marcarComoLeidoShouldNotSaveWhenAlreadyRead() {
        Mensaje mensaje = Mensaje.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario))
                .thenReturn(Optional.of(new MensajeLeido()));

        mensajeLeidoService.marcarComoLeido(mensaje, usuario);

        verify(mensajeLeidoRepository, never()).save(any());
    }

    @Test
    void estaLeidoShouldReturnTrueWhenRead() {
        Mensaje mensaje = Mensaje.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario))
                .thenReturn(Optional.of(new MensajeLeido()));

        assertThat(mensajeLeidoService.estaLeido(mensaje, usuario)).isTrue();
    }

    @Test
    void estaLeidoShouldReturnFalseWhenNotRead() {
        Mensaje mensaje = Mensaje.builder().id(1L).contenido("Hola").build();
        Usuario usuario = buildUsuario(1L);

        when(mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario))
                .thenReturn(Optional.empty());

        assertThat(mensajeLeidoService.estaLeido(mensaje, usuario)).isFalse();
    }
}
