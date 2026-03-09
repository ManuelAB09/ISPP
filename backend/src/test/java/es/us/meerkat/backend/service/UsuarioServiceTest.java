package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.us.meerkat.backend.dto.ChangePasswordRequest;
import es.us.meerkat.backend.dto.UpdateUserRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import es.us.meerkat.backend.dto.VisibilityRequest;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;

    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioService usuarioService;

    @Test
    void obtenerPerfilPropioShouldReturnPersistedUserProfile() {
        Usuario principal = new Usuario();
        principal.setEmail("user@meerkat.es");

        Usuario persisted = new Usuario();
        persisted.setId(1L);
        persisted.setEmail("user@meerkat.es");
        persisted.setNombre("Nombre Usuario");
        persisted.setVisibleEnListados(true);
        persisted.setEsTutor(false);
        persisted.setIntereses(List.of("java", "spring"));

        when(usuarioRepository.findByEmail(principal.getEmail()))
                .thenReturn(Optional.of(persisted));

        UserDetailResponse response = usuarioService.obtenerPerfilPropio(principal);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@meerkat.es");
        assertThat(response.getNombre()).isEqualTo("Nombre Usuario");
        assertThat(response.getIntereses()).containsExactly("java", "spring");
    }

    @Test
    void obtenerPerfilPropioShouldThrowWhenUserNotFound() {
        Usuario principal = new Usuario();
        principal.setEmail("missing@meerkat.es");

        when(usuarioRepository.findByEmail(principal.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPerfilPropio(principal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void actualizarPerfilShouldUpdateProvidedFieldsAndSave() {
        Usuario usuario = new Usuario();
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre("Sevilla");
        ubicacion.setCoste("100");
        ubicacion.setLatitud(24.0);
        ubicacion.setLongitud(42.0);
        ubicacion.setDireccion("Casa");

        usuario.setEmail("user@meerkat.es");
        usuario.setNombre("Nombre anterior");

        when(ubicacionRepository.findByNombre("Sevilla")).thenReturn(Optional.of(ubicacion));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombre("Nombre nuevo");
        request.setFoto("https://img.com/foto.png");
        request.setBio("Nueva bio");
        request.setUniversidad("US");
        request.setGrado("Ingeniería");
        request.setUbicacion(ubicacion.getNombre());
        request.setIntereses(List.of("backend", "arquitectura"));

        UserDetailResponse response = usuarioService.actualizarPerfil(usuario, request);

        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(usuario.getFoto()).isEqualTo("https://img.com/foto.png");
        assertThat(usuario.getBio()).isEqualTo("Nueva bio");
        assertThat(usuario.getUniversidad()).isEqualTo("US");
        assertThat(usuario.getGrado()).isEqualTo("Ingeniería");
        assertThat(usuario.getUbicacion().getNombre()).isEqualTo("Sevilla");
        assertThat(usuario.getIntereses()).containsExactly("backend", "arquitectura");

        assertThat(response.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(response.getBio()).isEqualTo("Nueva bio");
    }

    @Test
    void eliminarCuentaShouldDeleteUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);

        usuarioService.eliminarCuenta(usuario);

        verify(miembroComunidadRepository).deleteByUsuarioId(12L);
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void cambiarPasswordShouldSaveEncodedPasswordWhenDataIsValid() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("newPassword123");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encoded-new-password");

        usuarioService.cambiarPassword(usuario, request);

        assertThat(usuario.getPassword()).isEqualTo("encoded-new-password");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarPasswordShouldThrowWhenCurrentPasswordIsIncorrect() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-current");
        request.setNewPassword("newPassword123");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La contraseña actual es incorrecta");

        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void cambiarPasswordShouldThrowWhenNewPasswordIsTooShort() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("short");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La nueva contraseña debe tener al menos 8 caracteres");

        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void actualizarVisibilidadShouldSaveWhenVisibilityIsProvided() {
        Usuario usuario = new Usuario();
        usuario.setVisibleEnListados(true);

        VisibilityRequest request = new VisibilityRequest();
        request.setVisibleEnListados(false);

        UserDetailResponse response = usuarioService.actualizarVisibilidad(usuario, request);

        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getVisibleEnListados()).isFalse();
        assertThat(response.getVisibleEnListados()).isFalse();
    }

    @Test
    void actualizarVisibilidadShouldNotSaveWhenVisibilityIsNull() {
        Usuario usuario = new Usuario();
        usuario.setVisibleEnListados(true);

        VisibilityRequest request = new VisibilityRequest();
        request.setVisibleEnListados(null);

        UserDetailResponse response = usuarioService.actualizarVisibilidad(usuario, request);

        verify(usuarioRepository, never()).save(usuario);
        assertThat(response.getVisibleEnListados()).isTrue();
    }

    @Test
    void obtenerPerfilPublicoShouldReturnPublicProfile() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Perfil público");
        usuario.setFoto("https://img.com/public.png");
        usuario.setBio("Bio pública");
        usuario.setIntereses(List.of("testing"));
        usuario.setEsTutor(false);

        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));

        UserPublicResponse response = usuarioService.obtenerPerfilPublico(99L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getNombre()).isEqualTo("Perfil público");
        assertThat(response.getBio()).isEqualTo("Bio pública");
        assertThat(response.getIntereses()).containsExactly("testing");
    }

    @Test
    void obtenerPerfilPublicoShouldThrowWhenUserDoesNotExist() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPerfilPublico(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }
}
