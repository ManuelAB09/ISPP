package es.us.meerkat.backend.service.google;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.google.ComunidadClassroom;
import es.us.meerkat.backend.entity.google.GoogleClassroomConnection;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.google.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.google.GoogleClassroomConnectionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.google.GoogleClassroomService.OAuthCtx;

@ExtendWith(MockitoExtension.class)
class GoogleClassroomServiceTest {

    @Mock private GoogleClassroomConnectionRepository connectionRepository;
    @Mock private ComunidadClassroomRepository comunidadClassroomRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RestTemplate mockRestTemplate;

    private GoogleClassroomService service;
    private Usuario usuario;
    private Comunidad comunidad;

    @BeforeEach
    void setUp() {
        service =
                new GoogleClassroomService(
                        connectionRepository,
                        comunidadClassroomRepository,
                        comunidadRepository,
                        usuarioRepository);
        ReflectionTestUtils.setField(service, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "clientId", "test-client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(service, "redirectUri", "https://test.redirect/callback");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Test User");

        comunidad = new Comunidad();
        comunidad.setId(10L);
        comunidad.setNombre("Test Community");
    }

    // ============= generateState =============

    @Test
    void generateStateShouldReturnNonNullString() {
        String state = service.generateState();
        assertThat(state).isNotNull().isNotBlank();
    }

    @Test
    void generateStateShouldReturnUniqueValues() {
        String s1 = service.generateState();
        String s2 = service.generateState();
        assertThat(s1).isNotEqualTo(s2);
    }

    // ============= buildAuthorizeUrlForUser =============

    @Test
    void buildAuthorizeUrl_management_shouldContainRosterScope() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        String url = service.buildAuthorizeUrlForUser(1L, 10L, null, true);
        assertThat(url).contains("accounts.google.com").contains("classroom.rosters");
    }

    @Test
    void buildAuthorizeUrl_nonManagement_shouldContainReadonlyScopes() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        String url = service.buildAuthorizeUrlForUser(1L, 10L, null, false);
        assertThat(url).contains("accounts.google.com").contains("classroom.courses.readonly");
    }

    @Test
    void buildAuthorizeUrl_userNotFound_shouldThrow() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buildAuthorizeUrlForUser(99L, null, null, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void buildAuthorizeUrl_shouldStoreOAuthState() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        service.buildAuthorizeUrlForUser(1L, 10L, 5L, true);
        assertThat(service.oauthStateStore).isNotEmpty();
        OAuthCtx ctx = service.oauthStateStore.values().iterator().next();
        assertThat(ctx.userId()).isEqualTo(1L);
        assertThat(ctx.communityId()).isEqualTo(10L);
        assertThat(ctx.requestId()).isEqualTo(5L);
    }

    @Test
    void buildAuthorizeUrl_shouldContainClientIdAndRedirectUri() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        String url = service.buildAuthorizeUrlForUser(1L, null, null, false);
        assertThat(url).contains("client_id=test-client-id");
    }

    // ============= consumeState =============

    @Test
    void consumeState_shouldReturnAndRemoveContext() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        service.buildAuthorizeUrlForUser(1L, 10L, null, false);
        String state = service.oauthStateStore.keySet().iterator().next();

        OAuthCtx ctx = service.consumeState(state);
        assertThat(ctx).isNotNull();
        assertThat(ctx.userId()).isEqualTo(1L);
        assertThat(service.oauthStateStore).isEmpty();
    }

    @Test
    void consumeState_nonExistent_shouldReturnNull() {
        assertThat(service.consumeState("nonexistent")).isNull();
    }

    // ============= guardarConexionOAuth =============

    @Test
    void guardarConexionOAuth_newConnection_shouldCreate() {
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        service.guardarConexionOAuth(usuario, "access-token", "refresh-token", 3600);
        verify(connectionRepository)
                .save(
                        argThat(
                                c ->
                                        c.getAccessToken().equals("access-token")
                                                && c.getRefreshToken().equals("refresh-token")
                                                && c.getActiva()));
    }

    @Test
    void guardarConexionOAuth_existingConnection_shouldUpdate() {
        GoogleClassroomConnection existing =
                GoogleClassroomConnection.builder()
                        .id(1L)
                        .usuario(usuario)
                        .accessToken("old")
                        .refreshToken("old-refresh")
                        .expiresAt(Instant.now())
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(existing));

        service.guardarConexionOAuth(usuario, "new-access", "new-refresh", 3600);
        verify(connectionRepository).save(argThat(c -> c.getAccessToken().equals("new-access")));
    }

    @Test
    void guardarConexionOAuth_nullRefreshToken_shouldKeepExisting() {
        GoogleClassroomConnection existing =
                GoogleClassroomConnection.builder()
                        .id(1L)
                        .usuario(usuario)
                        .accessToken("old")
                        .refreshToken("keep-this")
                        .expiresAt(Instant.now())
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(existing));

        service.guardarConexionOAuth(usuario, "new-access", null, 3600);
        verify(connectionRepository).save(argThat(c -> c.getRefreshToken().equals("keep-this")));
    }

    @Test
    void guardarConexionOAuth_emptyRefreshToken_shouldKeepExisting() {
        GoogleClassroomConnection existing =
                GoogleClassroomConnection.builder()
                        .id(1L)
                        .usuario(usuario)
                        .accessToken("old")
                        .refreshToken("keep-this")
                        .expiresAt(Instant.now())
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(existing));

        service.guardarConexionOAuth(usuario, "new-access", "", 3600);
        verify(connectionRepository).save(argThat(c -> c.getRefreshToken().equals("keep-this")));
    }

    // ============= getAccessTokenValido =============

    @Test
    void getAccessTokenValido_notExpired_shouldReturnToken() {
        GoogleClassroomConnection connection =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("valid-token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(connection));

        assertThat(service.getAccessTokenValido(usuario)).isEqualTo("valid-token");
    }

    @Test
    void getAccessTokenValido_noConnection_shouldThrow() {
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAccessTokenValido(usuario))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getAccessTokenValido_expired_shouldRefreshAndReturn() {
        GoogleClassroomConnection connection =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("old-token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().minusSeconds(60))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(connection));

        Map<String, Object> body = Map.of("access_token", "new-token", "expires_in", 3600);
        ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(resp);

        String result = service.getAccessTokenValido(usuario);
        assertThat(result).isEqualTo("new-token");
    }

    // ============= refrescarAccessToken =============

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void refrescarAccessToken_success_shouldUpdateToken() {
        GoogleClassroomConnection connection =
                GoogleClassroomConnection.builder()
                        .id(1L)
                        .usuario(usuario)
                        .accessToken("old")
                        .refreshToken("refresh-token")
                        .expiresAt(Instant.now().minusSeconds(60))
                        .activa(true)
                        .build();

        Map<String, Object> body = Map.of("access_token", "new-token", "expires_in", 3600);
        ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(resp);

        service.refrescarAccessToken(connection);
        assertThat(connection.getAccessToken()).isEqualTo("new-token");
        verify(connectionRepository).save(connection);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void refrescarAccessToken_failure_shouldDeactivateAndThrow() {
        GoogleClassroomConnection connection =
                GoogleClassroomConnection.builder()
                        .id(1L)
                        .usuario(usuario)
                        .accessToken("old")
                        .refreshToken("refresh-token")
                        .expiresAt(Instant.now().minusSeconds(60))
                        .activa(true)
                        .build();

        ResponseEntity<Map> resp = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(resp);

        assertThatThrownBy(() -> service.refrescarAccessToken(connection))
                .isInstanceOf(RuntimeException.class);
        verify(connectionRepository).save(argThat(c -> !c.getActiva()));
    }

    // ============= vincularCurso =============

    @Test
    void vincularCurso_new_shouldCreate() {
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadClassroomRepository.findByComunidad(comunidad)).thenReturn(Optional.empty());
        when(comunidadClassroomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ComunidadClassroom result = service.vincularCurso(10L, "course-123", "My Course");
        assertThat(result.getClassroomCourseId()).isEqualTo("course-123");
        assertThat(result.getClassroomCourseName()).isEqualTo("My Course");
        assertThat(result.getActiva()).isTrue();
    }

    @Test
    void vincularCurso_existing_shouldUpdate() {
        ComunidadClassroom existing =
                ComunidadClassroom.builder()
                        .id(1L)
                        .comunidad(comunidad)
                        .classroomCourseId("old-id")
                        .classroomCourseName("Old Name")
                        .activa(false)
                        .build();
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadClassroomRepository.findByComunidad(comunidad))
                .thenReturn(Optional.of(existing));
        when(comunidadClassroomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ComunidadClassroom result = service.vincularCurso(10L, "new-id", "New Name");
        assertThat(result.getClassroomCourseId()).isEqualTo("new-id");
        assertThat(result.getActiva()).isTrue();
    }

    @Test
    void vincularCurso_communityNotFound_shouldThrow() {
        when(comunidadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vincularCurso(99L, "c", "n"))
                .isInstanceOf(RuntimeException.class);
    }

    // ============= desvincularCurso =============

    @Test
    void desvincularCurso_success_shouldDelete() {
        ComunidadClassroom cc = ComunidadClassroom.builder().id(1L).comunidad(comunidad).build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        service.desvincularCurso(10L);
        verify(comunidadClassroomRepository).delete(cc);
    }

    @Test
    void desvincularCurso_notFound_shouldThrow() {
        when(comunidadClassroomRepository.findByComunidadId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.desvincularCurso(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ============= getVinculacion =============

    @Test
    void getVinculacion_existing_shouldReturnPresent() {
        ComunidadClassroom cc = ComunidadClassroom.builder().id(1L).comunidad(comunidad).build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));
        assertThat(service.getVinculacion(10L)).isPresent();
    }

    @Test
    void getVinculacion_nonExisting_shouldReturnEmpty() {
        when(comunidadClassroomRepository.findByComunidadId(99L)).thenReturn(Optional.empty());
        assertThat(service.getVinculacion(99L)).isEmpty();
    }

    // ============= listarArchivosCursoVinculado =============

    @Test
    void listarArchivos_success_shouldReturnMaterialsAndCoursework() {
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        endsWith("/courseWorkMaterials"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"courseWorkMaterial\": []}", HttpStatus.OK));
        when(mockRestTemplate.exchange(
                        endsWith("/courseWork"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"courseWork\": []}", HttpStatus.OK));

        Map<String, Object> result = service.listarArchivosCursoVinculado(usuario, 10L);
        assertThat(result).containsKeys("materials", "courseWork");
    }

    @Test
    void listarArchivos_noVinculacion_shouldThrow() {
        when(comunidadClassroomRepository.findByComunidadId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listarArchivosCursoVinculado(usuario, 99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void listarArchivos_non2xxResponse_shouldReturnEmptyMaps() {
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        Map<String, Object> result = service.listarArchivosCursoVinculado(usuario, 10L);
        assertThat(result.get("materials")).isEqualTo(Map.of());
        assertThat(result.get("courseWork")).isEqualTo(Map.of());
    }

    // ============= obtenerEstadisticasAlumnos =============

    @Test
    void obtenerEstadisticas_success_shouldReturnStudentCount() {
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        String body = "{\"students\": [{\"userId\": \"u1\"}, {\"userId\": \"u2\"}]}";
        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Map<String, Object> result = service.obtenerEstadisticasAlumnos(usuario, 10L);
        assertThat(result.get("studentCount")).isEqualTo(2);
    }

    @Test
    void obtenerEstadisticas_noVinculacion_shouldThrow() {
        when(comunidadClassroomRepository.findByComunidadId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerEstadisticasAlumnos(usuario, 99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void obtenerEstadisticas_noStudentsKey_shouldReturnZero() {
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        Map<String, Object> result = service.obtenerEstadisticasAlumnos(usuario, 10L);
        assertThat(result.get("studentCount")).isEqualTo(0);
    }

    @Test
    void obtenerEstadisticas_non2xxResponse_shouldReturnZero() {
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .build();
        when(comunidadClassroomRepository.findByComunidadId(10L)).thenReturn(Optional.of(cc));

        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        Map<String, Object> result = service.obtenerEstadisticasAlumnos(usuario, 10L);
        assertThat(result.get("studentCount")).isEqualTo(0);
    }

    // ============= crearEstudiante =============

    @Test
    void crearEstudiante_success_shouldReturnResponse() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        String responseBody =
                "{\"id\":\"1\",\"userId\":\"u1\","
                        + "\"fullName\":\"John\",\"emailAddress\":\"john@test.com\"}";
        when(mockRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        var result = service.crearEstudiante(usuario, "course-1", "{\"userId\":\"john@test.com\"}");
        assertThat(result.id()).isEqualTo("1");
        assertThat(result.fullName()).isEqualTo("John");
    }

    @Test
    void crearEstudiante_failure_shouldThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("error", HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.crearEstudiante(usuario, "course-1", "{}"))
                .isInstanceOf(RuntimeException.class);
    }

    // ============= crearProfesor =============

    @Test
    void crearProfesor_success_shouldReturnResponse() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        String responseBody =
                "{\"id\":\"2\",\"userId\":\"t1\","
                        + "\"fullName\":\"Prof\",\"emailAddress\":\"prof@test.com\"}";
        when(mockRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        var result = service.crearProfesor(usuario, "course-1", "{\"userId\":\"prof@test.com\"}");
        assertThat(result.fullName()).isEqualTo("Prof");
    }

    @Test
    void crearProfesor_failure_shouldThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("error", HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.crearProfesor(usuario, "course-1", "{}"))
                .isInstanceOf(RuntimeException.class);
    }

    // ============= eliminarEstudiante =============

    @Test
    void eliminarEstudiante_success_shouldNotThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertThatCode(() -> service.eliminarEstudiante(usuario, "course-1", "student@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void eliminarEstudiante_failure_shouldThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(
                        () -> service.eliminarEstudiante(usuario, "course-1", "student@test.com"))
                .isInstanceOf(RuntimeException.class);
    }

    // ============= eliminarProfesor =============

    @Test
    void eliminarProfesor_success_shouldNotThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertThatCode(() -> service.eliminarProfesor(usuario, "course-1", "prof@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void eliminarProfesor_failure_shouldThrow() {
        GoogleClassroomConnection conn =
                GoogleClassroomConnection.builder()
                        .usuario(usuario)
                        .accessToken("token")
                        .refreshToken("refresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .activa(true)
                        .build();
        when(connectionRepository.findByUsuario(usuario)).thenReturn(Optional.of(conn));

        when(mockRestTemplate.exchange(
                        anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.eliminarProfesor(usuario, "course-1", "prof@test.com"))
                .isInstanceOf(RuntimeException.class);
    }
}
