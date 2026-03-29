package es.us.meerkat.backend.service.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.chats.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.chats.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.chats.MensajeComunidad;
import es.us.meerkat.backend.entity.chats.MensajeComunidadLeido;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeComunidadLeidoRepository;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadServiceTest {

    @Mock private MensajeComunidadRepository mensajeComunidadRepository;
    @Mock private MensajeComunidadLeidoRepository mensajeComunidadLeidoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private PreferenciasNotificacionService preferenciasNotificacionService;
    @Mock private EmailService emailService;

    @InjectMocks private MensajeComunidadService service;

    private Usuario buildUsuario(Long id, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        return u;
    }

    private Comunidad buildComunidad(Long id) {
        Comunidad c = new Comunidad();
        c.setId(id);
        return c;
    }

    @Test
    void enviarMensajeShouldSaveAndReturnResponse() {
        Usuario usuario = buildUsuario(1L, "Test User");
        Comunidad comunidad = buildComunidad(10L);

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola comunidad");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of(2L));
        Usuario miembro = buildUsuario(2L, "Miembro");
        miembro.setEmail("miembro@test.com");
        when(usuarioRepository.findAllById(List.of(2L))).thenReturn(List.of(miembro));
        PreferenciasNotificacion preferencias = new PreferenciasNotificacion();
        preferencias.setEmailsActivados(true);
        preferencias.setNotificarMensajeComunidad(true);
        when(preferenciasNotificacionService.getOrCreate(2L)).thenReturn(preferencias);
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(100L);
                            return m;
                        });

        MensajeComunidadResponse response = service.enviarMensaje(1L, request);

        assertThat(response).isNotNull();
        verify(mensajeComunidadRepository).save(any(MensajeComunidad.class));
        verify(emailService).sendCommunityMessageEmail(any(), any(), any(), any());
    }

    @Test
    void enviarMensajeShouldNotSendEmailWhenPreferenceDisabled() {
        Usuario usuario = buildUsuario(1L, "Test User");
        Comunidad comunidad = buildComunidad(10L);
        Usuario miembro = buildUsuario(2L, "Miembro");
        miembro.setEmail("miembro@test.com");

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola comunidad");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of(2L));
        when(usuarioRepository.findAllById(List.of(2L))).thenReturn(List.of(miembro));

        PreferenciasNotificacion preferencias = new PreferenciasNotificacion();
        preferencias.setEmailsActivados(true);
        preferencias.setNotificarMensajeComunidad(false);
        when(preferenciasNotificacionService.getOrCreate(2L)).thenReturn(preferencias);

        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(101L);
                            return m;
                        });

        service.enviarMensaje(1L, request);

        verify(emailService, never()).sendCommunityMessageEmail(any(), any(), any(), any());
    }

    @Test
    void enviarMensajeShouldThrowWhenUserNotFound() {
        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void enviarMensajeShouldThrowWhenCommunityNotFound() {
        Usuario usuario = buildUsuario(1L, "Test User");
        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enviarMensaje(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Comunidad no encontrada");
    }

    @Test
    void enviarArchivoShouldThrowWhenContentIsNull() {
        assertThatThrownBy(
                        () ->
                                service.enviarArchivo(
                                        1L, 10L, "text", "file.jpg", "image/jpeg", 100L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El contenido del archivo es obligatorio");
    }

    @Test
    void enviarArchivoShouldThrowWhenContentIsEmpty() {
        assertThatThrownBy(
                        () ->
                                service.enviarArchivo(
                                        1L,
                                        10L,
                                        "text",
                                        "file.jpg",
                                        "image/jpeg",
                                        100L,
                                        new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El contenido del archivo es obligatorio");
    }

    @Test
    void obtenerArchivoShouldThrowWhenMessageNotInCommunity() {
        Usuario usuario = buildUsuario(1L, "Test");

        MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .id(100L)
                        .comunidad(buildComunidad(99L))
                        .archivoData(new byte[] {1, 2})
                        .archivoNombre("file.jpg")
                        .archivoMimeType("image/jpeg")
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> service.obtenerArchivo(1L, 10L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El mensaje no pertenece a la comunidad indicada");
    }

    @Test
    void obtenerArchivoShouldThrowWhenNoFileData() {
        Usuario usuario = buildUsuario(1L, "Test");
        Comunidad comunidad = buildComunidad(10L);

        MensajeComunidad mensaje =
                MensajeComunidad.builder().id(100L).comunidad(comunidad).archivoData(null).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> service.obtenerArchivo(1L, 10L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El mensaje no contiene archivo");
    }

    @Test
    void obtenerHistorialShouldReturnMessages() {
        when(mensajeComunidadRepository.findByComunidadIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());

        List<MensajeComunidadResponse> result = service.obtenerHistorial(10L);
        assertThat(result).isEmpty();
    }

    @Test
    void editarMensajeShouldThrowWhenMessageNotFound() {
        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editarMensaje(1L, 100L, "nuevo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mensaje no encontrado");
    }

    @Test
    void editarMensajeShouldThrowWhenNotAuthor() {
        Usuario otroUsuario = buildUsuario(2L, "Otro");
        MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .id(100L)
                        .usuario(otroUsuario)
                        .contenido("original")
                        .build();

        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> service.editarMensaje(1L, 100L, "editado"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No tienes permiso para editar este mensaje");
    }

    // ================================================================
    // enviarArchivo – happy path
    // ================================================================

    @Test
    void enviarArchivoShouldSaveMessageWithExplicitContent() {
        Usuario usuario = buildUsuario(1L, "U1");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("TestCom");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of());
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(5L);
                            return m;
                        });

        MensajeComunidadResponse res =
                service.enviarArchivo(
                        1L, 10L, "mi texto", "f.pdf", "application/pdf", 100L, new byte[] {1});

        assertThat(res).isNotNull();
        verify(mensajeComunidadRepository, times(2)).save(any(MensajeComunidad.class));
    }

    @Test
    void enviarArchivoShouldUseDefaultContentWhenBlank() {
        Usuario usuario = buildUsuario(1L, "U1");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("TestCom");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of());
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(6L);
                            return m;
                        });

        service.enviarArchivo(1L, 10L, null, "f.pdf", "application/pdf", 100L, new byte[] {1});

        verify(mensajeComunidadRepository, times(2)).save(any(MensajeComunidad.class));
    }

    // ================================================================
    // obtenerArchivo – happy path
    // ================================================================

    @Test
    void obtenerArchivoShouldReturnFileWhenValid() {
        Usuario usuario = buildUsuario(1L, "Test");
        Comunidad comunidad = buildComunidad(10L);

        MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .id(100L)
                        .comunidad(comunidad)
                        .archivoData(new byte[] {1, 2, 3})
                        .archivoNombre("pic.png")
                        .archivoMimeType("image/png")
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        MensajeComunidadService.MensajeComunidadArchivo arch =
                service.obtenerArchivo(1L, 10L, 100L);

        assertThat(arch.data()).hasSize(3);
        assertThat(arch.nombre()).isEqualTo("pic.png");
    }

    // ================================================================
    // editarMensaje – happy path
    // ================================================================

    @Test
    void editarMensajeShouldUpdateContentWhenAuthor() {
        Usuario author = buildUsuario(1L, "Author");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("C");

        MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .id(100L)
                        .usuario(author)
                        .comunidad(comunidad)
                        .contenido("original")
                        .editado(false)
                        .build();

        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MensajeComunidadResponse res = service.editarMensaje(1L, 100L, "editado");

        assertThat(res.getContenido()).isEqualTo("editado");
    }

    // ================================================================
    // eliminarMensaje
    // ================================================================

    @Test
    void eliminarMensajeShouldDeleteWhenAuthor() {
        Usuario author = buildUsuario(1L, "Author");
        MensajeComunidad mensaje =
                MensajeComunidad.builder().id(100L).usuario(author).contenido("bye").build();

        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        service.eliminarMensaje(1L, 100L);

        verify(mensajeComunidadRepository).delete(mensaje);
    }

    @Test
    void eliminarMensajeShouldThrowWhenNotAuthor() {
        Usuario other = buildUsuario(2L, "Other");
        MensajeComunidad mensaje =
                MensajeComunidad.builder().id(100L).usuario(other).contenido("bye").build();

        when(mensajeComunidadRepository.findById(100L)).thenReturn(Optional.of(mensaje));

        assertThatThrownBy(() -> service.eliminarMensaje(1L, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void eliminarMensajeShouldThrowWhenNotFound() {
        when(mensajeComunidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarMensaje(1L, 99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ================================================================
    // marcarComunidadComoLeida
    // ================================================================

    @Test
    void marcarComunidadComoLeidaShouldCreateReadMarks() {
        Usuario autor = buildUsuario(2L, "Autor");
        Comunidad comunidad = buildComunidad(10L);

        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(
                        Optional.of(
                                new es.us.meerkat.backend.entity.communities.MiembroComunidad()));

        MensajeComunidad m1 =
                MensajeComunidad.builder().id(50L).usuario(autor).comunidad(comunidad).build();
        when(mensajeComunidadRepository.findByComunidadIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(m1));
        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(any(), any()))
                .thenReturn(Optional.empty());

        service.marcarComunidadComoLeida(1L, 10L);

        verify(mensajeComunidadLeidoRepository).save(any(MensajeComunidadLeido.class));
    }

    @Test
    void marcarComunidadComoLeidaShouldSkipAlreadyRead() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(
                        Optional.of(
                                new es.us.meerkat.backend.entity.communities.MiembroComunidad()));

        MensajeComunidad m1 =
                MensajeComunidad.builder()
                        .id(50L)
                        .usuario(buildUsuario(2L, "A"))
                        .comunidad(buildComunidad(10L))
                        .build();
        when(mensajeComunidadRepository.findByComunidadIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(m1));
        when(mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(any(), any()))
                .thenReturn(Optional.of(new MensajeComunidadLeido()));

        service.marcarComunidadComoLeida(1L, 10L);

        verify(mensajeComunidadLeidoRepository, never()).save(any());
    }

    @Test
    void marcarComunidadComoLeidaShouldThrowWhenNotMember() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarComunidadComoLeida(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comunidad");
    }

    // ================================================================
    // obtenerNoLeidosPorComunidad
    // ================================================================

    @Test
    void obtenerNoLeidosPorComunidadShouldReturnMap() {
        when(mensajeComunidadRepository.countNoLeidosByComunidadParaUsuario(1L))
                .thenReturn(List.of(new Object[] {10L, 3L}, new Object[] {20L, 1L}));

        java.util.Map<Long, Integer> result = service.obtenerNoLeidosPorComunidad(1L);

        assertThat(result).containsEntry(10L, 3).containsEntry(20L, 1);
    }

    @Test
    void obtenerNoLeidosPorComunidadShouldReturnEmptyMapWhenNone() {
        when(mensajeComunidadRepository.countNoLeidosByComunidadParaUsuario(1L))
                .thenReturn(List.of());

        java.util.Map<Long, Integer> result = service.obtenerNoLeidosPorComunidad(1L);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // enviarMensaje with mention (triggers sendCommunityMentionEmail)
    // ================================================================

    @Test
    void enviarMensajeShouldSendMentionEmailWhenUserMentioned() {
        Usuario usuario = buildUsuario(1L, "Sender");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("TestCom");

        Usuario miembro = buildUsuario(2L, "Carlos");
        miembro.setEmail("carlos@test.com");

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola @Carlos mira esto");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of(2L));
        when(usuarioRepository.findAllById(List.of(2L))).thenReturn(List.of(miembro));

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setEmailsActivados(true);
        prefs.setNotificarMenciones(true);
        when(preferenciasNotificacionService.getOrCreate(2L)).thenReturn(prefs);

        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(200L);
                            return m;
                        });

        service.enviarMensaje(1L, request);

        verify(emailService)
                .sendCommunityMentionEmail(eq(miembro), eq(comunidad), eq(usuario), any());
    }

    @Test
    void enviarMensajeShouldSkipSenderInNotifications() {
        Usuario usuario = buildUsuario(1L, "Sender");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("TestCom");

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola a todos");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        // only the sender is in the community
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of(1L));
        when(usuarioRepository.findAllById(List.of(1L))).thenReturn(List.of(usuario));
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(201L);
                            return m;
                        });

        service.enviarMensaje(1L, request);

        verify(emailService, never()).sendCommunityMessageEmail(any(), any(), any(), any());
        verify(emailService, never()).sendCommunityMentionEmail(any(), any(), any(), any());
    }

    @Test
    void enviarMensajeShouldNotNotifyWhenEmailsDisabled() {
        Usuario usuario = buildUsuario(1L, "Sender");
        Comunidad comunidad = buildComunidad(10L);
        comunidad.setNombre("TestCom");
        Usuario miembro = buildUsuario(2L, "M");
        miembro.setEmail("m@test.com");

        EnviarMensajeComunidadRequest request = new EnviarMensajeComunidadRequest();
        request.setComunidadId(10L);
        request.setContenido("Hola");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(10L)).thenReturn(List.of(2L));
        when(usuarioRepository.findAllById(List.of(2L))).thenReturn(List.of(miembro));
        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setEmailsActivados(false);
        when(preferenciasNotificacionService.getOrCreate(2L)).thenReturn(prefs);
        when(mensajeComunidadRepository.save(any(MensajeComunidad.class)))
                .thenAnswer(
                        inv -> {
                            MensajeComunidad m = inv.getArgument(0);
                            m.setId(202L);
                            return m;
                        });

        service.enviarMensaje(1L, request);

        verify(emailService, never()).sendCommunityMessageEmail(any(), any(), any(), any());
    }

    // ================================================================
    // obtenerHistorial – happy path
    // ================================================================

    @Test
    void obtenerHistorialShouldReturnMappedMessages() {
        Usuario author = buildUsuario(1L, "A");
        Comunidad com = buildComunidad(10L);
        com.setNombre("C");

        MensajeComunidad m =
                MensajeComunidad.builder()
                        .id(1L)
                        .contenido("Hola")
                        .usuario(author)
                        .comunidad(com)
                        .editado(false)
                        .build();

        when(mensajeComunidadRepository.findByComunidadIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(m));

        List<MensajeComunidadResponse> result = service.obtenerHistorial(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContenido()).isEqualTo("Hola");
    }
}
