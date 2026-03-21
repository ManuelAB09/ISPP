package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import es.us.meerkat.backend.dto.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.MensajeComunidad;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadServiceTest {

    @Mock private MensajeComunidadRepository mensajeComunidadRepository;
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
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(miembro));
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
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(miembro));

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
}
