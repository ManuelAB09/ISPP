package es.us.meerkat.backend.service.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import es.us.meerkat.backend.dto.chats.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.chats.MensajeResponse;
import es.us.meerkat.backend.entity.chats.Mensaje;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeLeidoRepository;
import es.us.meerkat.backend.repository.chats.MensajeRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock private MensajeRepository mensajeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MensajeLeidoRepository mensajeLeidoRepository;

    @InjectMocks private MensajeService mensajeService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nombre("User " + id)
                .email("u" + id + "@test.es")
                .password("p")
                .build();
    }

    private Mensaje buildMensaje(Long id, Usuario emisor, Usuario receptor) {
        Mensaje m = new Mensaje();
        m.setId(id);
        m.setContenido("Contenido " + id);
        m.setEmisor(emisor);
        m.setReceptor(receptor);
        m.setEditado(false);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    // ================================================================
    // enviarMensaje
    // ================================================================

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

        Usuario sender = buildUsuario(1L);
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

        Usuario sender = buildUsuario(1L);
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

        Usuario sender = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje savedMessage = buildMensaje(1L, sender, receptor);

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

        Usuario sender = buildUsuario(1L);
        Usuario receptorTutor = buildUsuario(2L);

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(receptorTutor);
        tutor.setVerificado(true);

        Mensaje savedMessage = buildMensaje(2L, sender, receptorTutor);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(savedMessage);

        mensajeService.enviarMensaje(1L, request);

        verify(mensajeRepository).save(any(Mensaje.class));
    }

    @Test
    void enviarMensajeShouldThrowWhenNoUserIdOrTutorId() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hello");

        Usuario sender = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId o tutorId");
    }

    @Test
    void enviarMensajeShouldThrowWhenTutorNotVerified() {
        EnviarMensajeRequest request = new EnviarMensajeRequest();
        request.setContenido("Hola");
        request.setTutorId(1L);

        Usuario sender = buildUsuario(1L);
        Usuario tutorUser = buildUsuario(2L);

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorUser);
        tutor.setVerificado(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> mensajeService.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no verificado");
    }

    // ================================================================
    // eliminarMensaje
    // ================================================================

    @Test
    void eliminarMensajeShouldDeleteWhenOwner() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje mensaje = buildMensaje(10L, emisor, receptor);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(mensaje));

        mensajeService.eliminarMensaje(1L, 10L);

        verify(mensajeRepository).delete(mensaje);
    }

    @Test
    void eliminarMensajeShouldThrowWhenNotOwner() {
        Usuario emisor = buildUsuario(2L);
        Usuario receptor = buildUsuario(1L);
        Mensaje mensaje = buildMensaje(10L, emisor, receptor);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> mensajeService.eliminarMensaje(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void eliminarMensajeShouldThrowWhenNotFound() {
        when(mensajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.eliminarMensaje(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrado");
    }

    // ================================================================
    // editarMensaje
    // ================================================================

    @Test
    void editarMensajeShouldUpdateContent() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje mensaje = buildMensaje(10L, emisor, receptor);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MensajeResponse result = mensajeService.editarMensaje(1L, 10L, "Editado");

        assertThat(mensaje.getContenido()).isEqualTo("Editado");
        assertThat(mensaje.getEditado()).isTrue();
        verify(mensajeRepository).save(mensaje);
    }

    @Test
    void editarMensajeShouldThrowWhenBlankContent() {
        assertThatThrownBy(() -> mensajeService.editarMensaje(1L, 10L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void editarMensajeShouldThrowWhenNotOwner() {
        Usuario emisor = buildUsuario(2L);
        Usuario receptor = buildUsuario(1L);
        Mensaje mensaje = buildMensaje(10L, emisor, receptor);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> mensajeService.editarMensaje(1L, 10L, "Edit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permiso");
    }

    // ================================================================
    // obtenerConversacion (by tutorId)
    // ================================================================

    @Test
    void obtenerConversacionShouldReturnMappedMessages() {
        Usuario user = buildUsuario(1L);
        Usuario tutorUser = buildUsuario(2L);
        Tutor tutor = new Tutor();
        tutor.setId(5L);
        tutor.setUsuario(tutorUser);

        Mensaje m1 = buildMensaje(1L, user, tutorUser);

        when(tutorRepository.findById(5L)).thenReturn(Optional.of(tutor));
        when(mensajeRepository.findConversationWithTutor(5L, 1L, 2L)).thenReturn(List.of(m1));

        List<MensajeResponse> result = mensajeService.obtenerConversacion(1L, 5L);

        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerConversacionShouldThrowWhenTutorNotFound() {
        when(tutorRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.obtenerConversacion(1L, 5L))
                .isInstanceOf(RuntimeException.class);
    }

    // ================================================================
    // obtenerConversacionConUsuario
    // ================================================================

    @Test
    void obtenerConversacionConUsuarioShouldReturnMessages() {
        Usuario user1 = buildUsuario(1L);
        Usuario user2 = buildUsuario(2L);
        Mensaje m1 = buildMensaje(1L, user1, user2);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(mensajeRepository.findConversationBetweenUsers(1L, 2L)).thenReturn(List.of(m1));

        List<MensajeResponse> result = mensajeService.obtenerConversacionConUsuario(1L, 2L);

        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerConversacionConUsuarioShouldThrowWhenNullOtherUser() {
        assertThatThrownBy(() -> mensajeService.obtenerConversacionConUsuario(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtenerConversacionConUsuarioShouldThrowWhenOtherUserNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.obtenerConversacionConUsuario(1L, 99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ================================================================
    // obtenerArchivo
    // ================================================================

    @Test
    void obtenerArchivoShouldReturnFileWhenAuthorized() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje m = buildMensaje(10L, emisor, receptor);
        m.setArchivoData(new byte[] {1, 2, 3});
        m.setArchivoNombre("file.pdf");
        m.setArchivoMimeType("application/pdf");

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(m));

        MensajeService.MensajeArchivo archivo = mensajeService.obtenerArchivo(1L, 10L);

        assertThat(archivo.data()).hasSize(3);
        assertThat(archivo.nombre()).isEqualTo("file.pdf");
    }

    @Test
    void obtenerArchivoShouldThrowWhenNotAuthorized() {
        Usuario emisor = buildUsuario(2L);
        Usuario receptor = buildUsuario(3L);
        Mensaje m = buildMensaje(10L, emisor, receptor);
        m.setArchivoData(new byte[] {1});

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> mensajeService.obtenerArchivo(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void obtenerArchivoShouldThrowWhenNoFileData() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje m = buildMensaje(10L, emisor, receptor);
        m.setArchivoData(null);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> mensajeService.obtenerArchivo(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("archivo");
    }

    // ================================================================
    // enviarArchivo
    // ================================================================

    @Test
    void enviarArchivoShouldThrowWhenDataIsNull() {
        assertThatThrownBy(
                        () ->
                                mensajeService.enviarArchivo(
                                        1L, 2L, null, "text", "f.txt", "text/plain", 100L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("archivo");
    }

    @Test
    void enviarArchivoShouldCreateMessageWithFile() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje saved = buildMensaje(10L, emisor, receptor);
        saved.setArchivoNombre("doc.pdf");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(receptor));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(saved);

        MensajeResponse result =
                mensajeService.enviarArchivo(
                        1L, 2L, null, null, "doc.pdf", "application/pdf", 5000L, new byte[] {1, 2});

        verify(mensajeRepository, times(2)).save(any(Mensaje.class));
    }

    @Test
    void enviarArchivoShouldThrowWhenSelfSend() {
        Usuario emisor = buildUsuario(1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(emisor));

        assertThatThrownBy(
                        () ->
                                mensajeService.enviarArchivo(
                                        1L,
                                        1L,
                                        null,
                                        "c",
                                        "f.txt",
                                        "text/plain",
                                        10L,
                                        new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ti mismo");
    }

    // ================================================================
    // obtenerMensaje
    // ================================================================

    @Test
    void obtenerMensajeShouldReturnResponse() {
        Usuario emisor = buildUsuario(1L);
        Usuario receptor = buildUsuario(2L);
        Mensaje m = buildMensaje(10L, emisor, receptor);

        when(mensajeRepository.findById(10L)).thenReturn(Optional.of(m));

        MensajeResponse result = mensajeService.obtenerMensaje(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void obtenerMensajeShouldThrowWhenNotFound() {
        when(mensajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.obtenerMensaje(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // obtenerConversaciones
    // ================================================================

    @Test
    void obtenerConversacionesShouldThrowWhenUserNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.obtenerConversaciones(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void obtenerConversacionesShouldReturnEmptyWhenNoMessages() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(buildUsuario(1L)));
        when(mensajeRepository.findAllConversations(1L)).thenReturn(List.of());

        List<?> result = mensajeService.obtenerConversaciones(1L);

        assertThat(result).isEmpty();
    }
}
