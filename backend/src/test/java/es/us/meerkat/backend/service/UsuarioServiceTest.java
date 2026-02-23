package es.us.meerkat.backend.service;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.CambiarPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.PrivacidadRequest;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.UpdatePerfilRequest;
import es.us.meerkat.backend.dto.UsuarioPerfilResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link UsuarioService}.
 *
 * Se mockean el repositorio y el encoder para aislar
 * completamente la lógica de negocio del servicio.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    /** Usuario de prueba reutilizado en varios tests. */
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setEmail("test@meerkat.com");
        usuarioMock.setPassword("hashedPassword");
        usuarioMock.setNombre("Test Usuario");
        usuarioMock.setEsTutor(false);
        usuarioMock.setVisibleEnListados(true);
    }

    // ===============================
    // REGISTRAR
    // ===============================

    @Test
    void registrar_conDatosValidos_retornaMensajeExito() {
        final RegisterRequest request = new RegisterRequest();
        request.setEmail("nuevo@meerkat.com");
        request.setPassword("password123");
        request.setNombre("Nuevo Usuario");

        when(usuarioRepository.existsByEmail("nuevo@meerkat.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        final String resultado = usuarioService.registrar(request);

        assertThat(resultado).isEqualTo("Cuenta creada correctamente");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrar_conEmailYaEnUso_lanzaExcepcion() {
        final RegisterRequest request = new RegisterRequest();
        request.setEmail("test@meerkat.com");
        request.setPassword("password123");
        request.setNombre("Otro Usuario");

        when(usuarioRepository.existsByEmail("test@meerkat.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El email ya está en uso");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conEmailVacio_lanzaExcepcion() {
        final RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("password123");

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El email no puede estar vacío");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conPasswordCorta_lanzaExcepcion() {
        final RegisterRequest request = new RegisterRequest();
        request.setEmail("nuevo@meerkat.com");
        request.setPassword("corta");

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La contraseña debe tener al menos 8 caracteres");

        verify(usuarioRepository, never()).save(any());
    }

    // ===============================
    // INICIAR SESIÓN
    // ===============================

    @Test
    void iniciarSesion_conCredencialesCorrectas_retornaAuthResponse() {
        final LoginRequest request = new LoginRequest();
        request.setEmail("test@meerkat.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("test@meerkat.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        final AuthResponse respuesta = usuarioService.iniciarSesion(request);

        assertThat(respuesta.getEmail()).isEqualTo("test@meerkat.com");
        assertThat(respuesta.getNombre()).isEqualTo("Test Usuario");
        assertThat(respuesta.getId()).isEqualTo(1L);
    }

    @Test
    void iniciarSesion_conEmailInexistente_lanzaExcepcion() {
        final LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@meerkat.com");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail("noexiste@meerkat.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.iniciarSesion(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    void iniciarSesion_conPasswordIncorrecta_lanzaExcepcion() {
        final LoginRequest request = new LoginRequest();
        request.setEmail("test@meerkat.com");
        request.setPassword("wrongpassword");

        when(usuarioRepository.findByEmail("test@meerkat.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.iniciarSesion(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credenciales incorrectas");
    }

    // ===============================
    // ACTUALIZAR PERFIL
    // ===============================

    @Test
    void actualizarPerfil_conDatosValidos_actualizaCampos() {
        final UpdatePerfilRequest request = new UpdatePerfilRequest();
        request.setNombre("Nombre Nuevo");
        request.setBio("Bio nueva");
        request.setFoto("https://foto.com/nueva.jpg");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioMock);

        final UsuarioPerfilResponse respuesta = usuarioService.actualizarPerfil(1L, request);

        assertThat(respuesta.getNombre()).isEqualTo("Nombre Nuevo");
        assertThat(respuesta.getBio()).isEqualTo("Bio nueva");
        assertThat(respuesta.getFoto()).isEqualTo("https://foto.com/nueva.jpg");
        verify(usuarioRepository).save(usuarioMock);
    }

    @Test
    void actualizarPerfil_conCamposNulos_noSobreescribeCampos() {
        usuarioMock.setNombre("Nombre Original");
        usuarioMock.setBio("Bio original");

        final UpdatePerfilRequest request = new UpdatePerfilRequest();
        // nombre y bio son null — no deben sobreescribirse

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioMock);

        final UsuarioPerfilResponse respuesta = usuarioService.actualizarPerfil(1L, request);

        assertThat(respuesta.getNombre()).isEqualTo("Nombre Original");
        assertThat(respuesta.getBio()).isEqualTo("Bio original");
    }

    @Test
    void actualizarPerfil_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(99L, new UpdatePerfilRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    // ===============================
    // CAMBIAR CONTRASEÑA
    // ===============================

    @Test
    void cambiarPassword_conDatosValidos_actualizaPassword() {
        final CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("password123");
        request.setPasswordNueva("nuevaPassword123");
        request.setPasswordConfirmacion("nuevaPassword123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("nuevaPassword123")).thenReturn("newHashedPassword");

        final String resultado = usuarioService.cambiarPassword(1L, request);

        assertThat(resultado).isEqualTo("Contraseña actualizada correctamente");
        verify(usuarioRepository).save(usuarioMock);
    }

    @Test
    void cambiarPassword_conPasswordActualIncorrecta_lanzaExcepcion() {
        final CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("wrongPassword");
        request.setPasswordNueva("nuevaPassword123");
        request.setPasswordConfirmacion("nuevaPassword123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La contraseña actual es incorrecta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPassword_conConfirmacionNoCoincide_lanzaExcepcion() {
        final CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("password123");
        request.setPasswordNueva("nuevaPassword123");
        request.setPasswordConfirmacion("otraPassword123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La nueva contraseña y su confirmación no coinciden");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPassword_conNuevaPasswordCorta_lanzaExcepcion() {
        final CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual("password123");
        request.setPasswordNueva("corta");
        request.setPasswordConfirmacion("corta");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La nueva contraseña debe tener al menos 8 caracteres");

        verify(usuarioRepository, never()).save(any());
    }

    // ===============================
    // ELIMINAR CUENTA
    // ===============================

    @Test
    void eliminarCuenta_conUsuarioExistente_eliminaYRetornaMensaje() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        final String resultado = usuarioService.eliminarCuenta(1L);

        assertThat(resultado).isEqualTo("Cuenta eliminada permanentemente");
        verify(usuarioRepository).delete(usuarioMock);
    }

    @Test
    void eliminarCuenta_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminarCuenta(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");

        verify(usuarioRepository, never()).delete(any());
    }

    // ===============================
    // VER PERFIL PÚBLICO
    // ===============================

    @Test
    void verPerfil_conUsuarioExistente_retornaPerfil() {
        usuarioMock.setBio("Mi bio");
        usuarioMock.setFoto("https://foto.com/foto.jpg");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        final UsuarioPerfilResponse respuesta = usuarioService.verPerfil(1L);

        assertThat(respuesta.getId()).isEqualTo(1L);
        assertThat(respuesta.getNombre()).isEqualTo("Test Usuario");
        assertThat(respuesta.getBio()).isEqualTo("Mi bio");
    }

    @Test
    void verPerfil_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.verPerfil(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void listarPerfilesPublicos_retornaUsuariosVisibles() {
        final Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setNombre("Otro Usuario");
        usuario2.setVisibleEnListados(true);
        usuario2.setEsTutor(false);

        when(usuarioRepository.findByVisibleEnListadosTrue())
                .thenReturn(List.of(usuarioMock, usuario2));

        final List<UsuarioPerfilResponse> resultado = usuarioService.listarPerfilesPublicos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Test Usuario");
        assertThat(resultado.get(1).getNombre()).isEqualTo("Otro Usuario");
    }

    // ===============================
    // PRIVACIDAD
    // ===============================

    @Test
    void actualizarPrivacidad_aOculto_actualizaVisibilidad() {
        final PrivacidadRequest request = new PrivacidadRequest();
        request.setVisibleEnListados(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        final String resultado = usuarioService.actualizarPrivacidad(1L, request);

        assertThat(resultado).isEqualTo("Configuración de privacidad actualizada");
        assertThat(usuarioMock.getVisibleEnListados()).isFalse();
        verify(usuarioRepository).save(usuarioMock);
    }

    @Test
    void actualizarPrivacidad_aVisible_actualizaVisibilidad() {
        usuarioMock.setVisibleEnListados(false);

        final PrivacidadRequest request = new PrivacidadRequest();
        request.setVisibleEnListados(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        usuarioService.actualizarPrivacidad(1L, request);

        assertThat(usuarioMock.getVisibleEnListados()).isTrue();
        verify(usuarioRepository).save(usuarioMock);
    }

    @Test
    void actualizarPrivacidad_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarPrivacidad(99L, new PrivacidadRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");

        verify(usuarioRepository, never()).save(any());
    }
}
