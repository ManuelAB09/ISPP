package es.us.meerkat.backend.service.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.users.ChangePasswordRequest;
import es.us.meerkat.backend.dto.users.UpdateUserRequest;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.dto.users.VisibilityRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.communities.SolicitudComunidadRepository;
import es.us.meerkat.backend.repository.emails.RecordatorioEmailRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioRepository;
import es.us.meerkat.backend.repository.google.GoogleClassroomConnectionRepository;
import es.us.meerkat.backend.repository.maps.UbicacionRepository;
import es.us.meerkat.backend.repository.notifications.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.subscriptions.SuscripcionRepository;
import es.us.meerkat.backend.repository.subscriptions.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;

    @Mock private ComunidadRepository comunidadRepository;

    @Mock private SuscripcionRepository suscripcionRepository;

    @Mock private TransaccionPagoRepository transaccionPagoRepository;

    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;

    @Mock private EventoRepository eventoRepository;

    @Mock private SolicitudComunidadRepository solicitudComunidadRepository;

    @Mock private MensajeComunidadRepository mensajeComunidadRepository;

    @Mock private GoogleClassroomConnectionRepository googleClassroomConnectionRepository;

    @Mock private CuestionarioRepository cuestionarioRepository;

    @Mock private RecordatorioEmailRepository recordatorioEmailRepository;

    @Mock private InstitutionRepository institutionRepository;

    @Mock private PreferenciasNotificacionRepository preferenciasNotificacionRepository;

    @Mock private EntityManager entityManager;

    @Mock private BCryptPasswordEncoder passwordEncoder;

    @Mock private ResourcePatternResolver resourcePatternResolver;

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

        // Mock comunidades (no hay comunidades del usuario)
        when(comunidadRepository.findByCreadorId(12L)).thenReturn(List.of());

        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);

        usuarioService.eliminarCuenta(usuario);

        // Verificar que todas las dependencias fueron eliminadas en orden
        verify(asistenciaEventoRepository).deleteByUsuarioId(12L);
        verify(asistenciaEventoRepository).deleteByEventoCreadorId(12L);
        verify(eventoRepository).deleteByUsuarioId(12L);
        verify(mensajeComunidadRepository).deleteByUsuarioId(12L);
        verify(entityManager, times(2)).clear();
        verify(solicitudComunidadRepository).deleteBySolicitanteId(12L);
        verify(solicitudComunidadRepository).deleteByRespondidaPorId(12L);
        verify(googleClassroomConnectionRepository).deleteByUsuarioId(12L);
        verify(institutionRepository).deleteByUsuarioAdminId(12L);
        verify(transaccionPagoRepository).deleteByUsuarioId(12L);
        verify(suscripcionRepository).deleteByUsuarioId(12L);
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

    @Test
    void obtenerPerfilPublicoShouldIncludeUbicacionAndTutorId() {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(10L);
        ubicacion.setNombre("Sevilla");
        ubicacion.setDireccion("Calle Falsa 123");
        ubicacion.setLatitud(37.3886);
        ubicacion.setLongitud(-5.9823);
        ubicacion.setTipo("PRESENCIAL");
        ubicacion.setCoste("gratis");

        Tutor tutor = new Tutor();
        tutor.setId(20L);

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        usuario.setNombre("Con ubicación");
        usuario.setFoto("https://img.com/ubi.png");
        usuario.setBio("Bio con ubicación");
        usuario.setIntereses(List.of());
        usuario.setEsTutor(true);
        usuario.setUbicacion(ubicacion);
        usuario.setTutor(tutor);

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(usuario));

        UserPublicResponse response = usuarioService.obtenerPerfilPublico(50L);

        assertThat(response.getUbicacion()).isNotNull();
        assertThat(response.getUbicacion().getNombre()).isEqualTo("Sevilla");
        assertThat(response.getTutorId()).isEqualTo(20L);
    }

    // ── actualizarFotoPerfil ──────────────────────────────────────────────

    @Test
    void actualizarFotoPerfilShouldThrowWhenFileIsNull() {
        Usuario usuario = new Usuario();
        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archivo de imagen requerido");
    }

    @Test
    void actualizarFotoPerfilShouldThrowWhenFileIsEmpty() {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archivo de imagen requerido");
    }

    @Test
    void actualizarFotoPerfilShouldThrowWhenFileTooLarge() {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024L * 1024L); // 6MB

        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto supera el límite de 5MB");
    }

    @Test
    void actualizarFotoPerfilShouldThrowWhenMimeTypeIsNull() {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Solo JPG, PNG o WEBP");
    }

    @Test
    void actualizarFotoPerfilShouldThrowWhenMimeTypeNotAllowed() {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/gif");

        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Solo JPG, PNG o WEBP");
    }

    @Test
    void actualizarFotoPerfilShouldSaveBase64DataUriWhenValid() throws IOException {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@meerkat.es");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[] {1, 2, 3});

        UserDetailResponse response = usuarioService.actualizarFotoPerfil(usuario, file);

        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getFoto()).startsWith("data:image/png;base64,");
        assertThat(response).isNotNull();
    }

    @Test
    void actualizarFotoPerfilShouldThrowIllegalStateWhenIOException() throws IOException {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> usuarioService.actualizarFotoPerfil(usuario, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo procesar la imagen");
    }

    // ── cambiarPassword extra branches ────────────────────────────────────

    @Test
    void cambiarPasswordShouldThrowWhenNewPasswordTooLong() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("A1a" + "x".repeat(126)); // 129 chars

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La nueva contraseña no puede tener más de 128 caracteres");

        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void cambiarPasswordShouldThrowWhenMissingUppercase() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("nouppercase1");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe contener mayúsculas, minúsculas y números");
    }

    @Test
    void cambiarPasswordShouldThrowWhenMissingLowercase() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("NOLOWERCASE1");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe contener mayúsculas, minúsculas y números");
    }

    @Test
    void cambiarPasswordShouldThrowWhenMissingDigit() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("NoDigitHere");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe contener mayúsculas, minúsculas y números");
    }

    @Test
    void cambiarPasswordShouldThrowWhenSameAsOldPassword() {
        Usuario usuario = new Usuario();
        usuario.setPassword("encoded-current");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword("SamePassword1");

        when(passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarPassword(usuario, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La nueva contraseña no puede ser igual a la anterior");

        verify(usuarioRepository, never()).save(usuario);
    }

    // ── actualizarPerfil ubicacion branches ───────────────────────────────

    @Test
    void actualizarPerfilShouldSetUbicacionToNullWhenEmptyString() {
        Usuario usuario = new Usuario();
        Ubicacion old = new Ubicacion();
        old.setNombre("OldCity");
        usuario.setUbicacion(old);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUbicacion("   "); // blank → empty after trim

        usuarioService.actualizarPerfil(usuario, request);

        assertThat(usuario.getUbicacion()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarPerfilShouldCreateNewUbicacionWhenNotFoundInRepo() {
        Usuario usuario = new Usuario();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUbicacion("Madrid");

        when(ubicacionRepository.findByNombre("Madrid")).thenReturn(Optional.empty());

        Ubicacion savedUbicacion =
                Ubicacion.builder()
                        .nombre("Madrid")
                        .direccion("Madrid")
                        .latitud(0.0)
                        .longitud(0.0)
                        .tipo("general")
                        .coste("desconocido")
                        .build();
        when(ubicacionRepository.save(any(Ubicacion.class))).thenReturn(savedUbicacion);

        UserDetailResponse response = usuarioService.actualizarPerfil(usuario, request);

        verify(ubicacionRepository).findByNombre("Madrid");
        verify(ubicacionRepository).save(any(Ubicacion.class));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarPerfilShouldParseCoordinatesWhenUbicacionIsCoordPair() {
        Usuario usuario = new Usuario();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUbicacion("37.3886,-5.9823");

        when(ubicacionRepository.findByNombre("37.3886,-5.9823")).thenReturn(Optional.empty());

        Ubicacion savedUbicacion =
                Ubicacion.builder()
                        .nombre("37.3886,-5.9823")
                        .direccion("37.3886,-5.9823")
                        .latitud(37.3886)
                        .longitud(-5.9823)
                        .tipo("general")
                        .coste("desconocido")
                        .build();
        when(ubicacionRepository.save(any(Ubicacion.class))).thenReturn(savedUbicacion);

        usuarioService.actualizarPerfil(usuario, request);

        verify(ubicacionRepository).save(any(Ubicacion.class));
    }

    // ── actualizarPerfil additional field branches ──────────────────────

    @Test
    void actualizarPerfilShouldUpdateAllOptionalFields() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@meerkat.es");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEsTutor(true);
        request.setNivelEstudios("Máster");
        request.setBaseFormativa("Ingeniería");
        request.setAutenticacionDosFactores(true);
        request.setVisibleEnListados(false);
        request.setNotificacionesPush(false);
        request.setFotoBackgroundColor("#FF0000");

        UserDetailResponse response = usuarioService.actualizarPerfil(usuario, request);

        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getEsTutor()).isTrue();
        assertThat(usuario.getNivelEstudios()).isEqualTo("Máster");
        assertThat(usuario.getBaseFormativa()).isEqualTo("Ingeniería");
        assertThat(usuario.getAutenticacionDosFactores()).isTrue();
        assertThat(usuario.getVisibleEnListados()).isFalse();
        assertThat(usuario.getNotificacionesPush()).isFalse();
        assertThat(usuario.getFotoBackgroundColor()).isEqualTo("#FF0000");
    }

    // ── eliminarCuenta with tutor ─────────────────────────────────────

    @Test
    void eliminarCuentaShouldCleanupTutorDependenciesWhenUserIsTutor() {
        Usuario usuario = new Usuario();
        usuario.setId(15L);

        Tutor tutor = new Tutor();
        tutor.setId(30L);
        tutor.setUsuario(usuario);
        usuario.setTutor(tutor);

        when(comunidadRepository.findByCreadorId(15L)).thenReturn(List.of());

        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);

        usuarioService.eliminarCuenta(usuario);

        // Tutor JPQL cleanup queries should include tutor-specific ones
        verify(entityManager, times(2)).clear();
        verify(usuarioRepository).delete(usuario);
    }

    // ── actualizarPerfil additional field branches ──────────────────────

    @Test
    void obtenerAvataresPerfilDisponiblesShouldFilterNullFilenames() throws IOException {
        Resource r1 = mock(Resource.class);
        when(r1.getFilename()).thenReturn(null); // null filename
        Resource r2 = mock(Resource.class);
        when(r2.getFilename()).thenReturn("avatar.png");
        when(resourcePatternResolver.getResources(anyString())).thenReturn(new Resource[] {r1, r2});

        List<String> result = usuarioService.obtenerAvataresPerfilDisponibles();

        assertThat(result).containsExactly("/static/images/renata/avatar.png");
    }

    // ── actualizarFotoPerfil with webp format ─────────────────────────

    @Test
    void actualizarFotoPerfilShouldAcceptWebpFormat() throws IOException {
        Usuario usuario = new Usuario();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("image/webp");
        when(file.getBytes()).thenReturn(new byte[] {10, 20, 30});

        UserDetailResponse response = usuarioService.actualizarFotoPerfil(usuario, file);

        assertThat(usuario.getFoto()).startsWith("data:image/webp;base64,");
    }

    // ── obtenerAvataresPerfilDisponibles ──────────────────────────────────

    @Test
    void obtenerAvataresPerfilDisponiblesShouldReturnSortedPrefixedList() throws IOException {
        Resource r1 = mock(Resource.class);
        when(r1.getFilename()).thenReturn("avatar_b.png");
        Resource r2 = mock(Resource.class);
        when(r2.getFilename()).thenReturn("avatar_a.png");
        when(resourcePatternResolver.getResources(anyString())).thenReturn(new Resource[] {r1, r2});

        List<String> result = usuarioService.obtenerAvataresPerfilDisponibles();

        assertThat(result)
                .containsExactly(
                        "/static/images/renata/avatar_a.png", "/static/images/renata/avatar_b.png");
    }

    @Test
    void obtenerAvataresPerfilDisponiblesShouldReturnEmptyWhenNoResources() throws IOException {
        when(resourcePatternResolver.getResources(anyString())).thenReturn(new Resource[0]);

        List<String> result = usuarioService.obtenerAvataresPerfilDisponibles();

        assertThat(result).isEmpty();
    }

    @Test
    void obtenerAvataresPerfilDisponiblesShouldReturnEmptyWhenIOException() throws IOException {
        when(resourcePatternResolver.getResources(anyString())).thenThrow(new IOException("fail"));

        List<String> result = usuarioService.obtenerAvataresPerfilDisponibles();

        assertThat(result).isEmpty();
    }

    // ── eliminarCuenta community branches ──────────────────────────────

    @Test
    void eliminarCuentaShouldThrowWhenUsuarioIsNull() {
        assertThatThrownBy(() -> usuarioService.eliminarCuenta(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Usuario no autenticado");
    }

    @Test
    void eliminarCuentaShouldTransferCommunityToOldestEligibleMember() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);

        Comunidad comunidad =
                Comunidad.builder()
                        .id(1L)
                        .creador(usuario)
                        .tipoPlan(TipoPlanComunidad.FREE)
                        .build();
        when(comunidadRepository.findByCreadorId(12L)).thenReturn(List.of(comunidad));

        Usuario transferee = new Usuario();
        transferee.setId(20L);
        when(miembroComunidadRepository.findMiembrosMasAntiguosEnComunidad(1L, 12L))
                .thenReturn(List.of(transferee));
        when(comunidadRepository.countByCreadorIdAndTipoPlan(20L, TipoPlanComunidad.FREE))
                .thenReturn(0L);

        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);

        usuarioService.eliminarCuenta(usuario);

        assertThat(comunidad.getCreador()).isEqualTo(transferee);
        verify(comunidadRepository).save(comunidad);
        verify(comunidadRepository, never()).delete(comunidad);
    }

    @Test
    void eliminarCuentaShouldDeleteCommunityWhenNoMembers() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);

        Comunidad comunidad =
                Comunidad.builder()
                        .id(1L)
                        .creador(usuario)
                        .tipoPlan(TipoPlanComunidad.FREE)
                        .build();
        when(comunidadRepository.findByCreadorId(12L)).thenReturn(List.of(comunidad));
        when(miembroComunidadRepository.findMiembrosMasAntiguosEnComunidad(1L, 12L))
                .thenReturn(List.of());

        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);

        usuarioService.eliminarCuenta(usuario);

        verify(comunidadRepository).delete(comunidad);
    }

    @Test
    void eliminarCuentaShouldDeleteCommunityWhenAllMembersExceedFreeLimit() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);

        Comunidad comunidad =
                Comunidad.builder()
                        .id(1L)
                        .creador(usuario)
                        .tipoPlan(TipoPlanComunidad.FREE)
                        .build();
        when(comunidadRepository.findByCreadorId(12L)).thenReturn(List.of(comunidad));

        Usuario member = new Usuario();
        member.setId(20L);
        when(miembroComunidadRepository.findMiembrosMasAntiguosEnComunidad(1L, 12L))
                .thenReturn(List.of(member));
        // Member already at max free communities
        when(comunidadRepository.countByCreadorIdAndTipoPlan(20L, TipoPlanComunidad.FREE))
                .thenReturn(100L);

        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(0);

        usuarioService.eliminarCuenta(usuario);

        verify(comunidadRepository).delete(comunidad);
    }
}
