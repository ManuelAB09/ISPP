package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import es.us.meerkat.backend.dto.chats.EnviarMensajeRequest;
import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.chats.MensajeService;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock private MensajeRepository mensajeRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private TutorRepository tutorRepository;

    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private MensajeService mensajeService;

    @Test
    void enviarMensajeShouldThrowWhenContentIsBlank() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("");

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void enviarMensajeShouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void enviarMensajeShouldThrowWhenSenderNotFound() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hola mundo");
        request.setUserId(2L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void enviarMensajeShouldThrowWhenReceptorNotFound() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hola mundo");
        request.setUserId(2L);

        Usuario sender = new Usuario();
        sender.setId(1L);
        sender.setEmail("sender@meerkat.es");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario receptor no encontrado");
    }

    @Test
    void enviarMensajeShouldThrowWhenSenderSendsMessageToThemself() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hola mundo");
        request.setUserId(1L);

        Usuario sender = new Usuario();
        sender.setId(1L);
        sender.setEmail("sender@meerkat.es");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No puedes enviarte mensajes a ti mismo");
    }

    @Test
    void enviarMensajeShouldSuccessfullyCreateMessage() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hola mundo");
        request.setUserId(2L);

        Usuario sender = new Usuario();
        sender.setId(1L);
        sender.setEmail("sender@meerkat.es");
        sender.setNombre("Sender");

        Usuario receptor = new Usuario();
        receptor.setId(2L);
        receptor.setEmail("receptor@meerkat.es");
        receptor.setNombre("Receptor");

        Mensaje messageToSave =
                Mensaje.builder()
                        .contenido("Hola mundo")
                        .emisor(sender)
                        .receptor(receptor)
                        .tutor(null)
                        .build();

        Mensaje savedMessage = new Mensaje();
        savedMessage.setId(1L);
        savedMessage.setContenido("Hola mundo");
        savedMessage.setEmisor(sender);
        savedMessage.setReceptor(receptor);
        savedMessage.setTutor(null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(savedMessage);

        mensajeService.enviarMensaje(1L, request);

        verify(mensajeRepository).save(any(Mensaje.class));
    }

    @Test
    void enviarMensajeShouldWorkWithTutorId() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Necesito ayuda");
        request.setTutorId(1L);

        Usuario sender = new Usuario();
        sender.setId(1L);
        sender.setEmail("sender@meerkat.es");
        sender.setNombre("Sender");

        Usuario receptorTutor = new Usuario();
        receptorTutor.setId(2L);
        receptorTutor.setEmail("tutor@meerkat.es");
        receptorTutor.setNombre("Tutor");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(receptorTutor);
        tutor.setVerificado(true);

        Mensaje savedMessage = new Mensaje();
        savedMessage.setId(2L);
        savedMessage.setContenido("Necesito ayuda");
        savedMessage.setEmisor(sender);
        savedMessage.setReceptor(receptorTutor);
        savedMessage.setTutor(tutor);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(savedMessage);

        mensajeService.enviarMensaje(1L, request);

        verify(mensajeRepository).save(any(Mensaje.class));
    }
}
