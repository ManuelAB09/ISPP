package es.us.meerkat.backend.controller.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import es.us.meerkat.backend.dto.google.ClassroomUserResponse;
import es.us.meerkat.backend.dto.tutors.CreateStudentRequest;
import es.us.meerkat.backend.dto.tutors.CreateTeacherRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.google.ComunidadClassroom;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.google.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.google.GoogleClassroomService;

@ExtendWith(MockitoExtension.class)
class GoogleClassroomControllerTest {

    @Mock private GoogleClassroomService googleClassroomService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadClassroomRepository comunidadClassroomRepository;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private GoogleClassroomController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStudentShouldAllowWhenAdminOrProfesor() throws Exception {
        Usuario u = new Usuario();
        u.setId(2L);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(u);

        SecurityContext sc = org.mockito.Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        Comunidad comunidad = Comunidad.builder().id(5L).build();
        ComunidadClassroom cc = new ComunidadClassroom();
        cc.setComunidad(comunidad);

        when(comunidadClassroomRepository.findByClassroomCourseId("c1"))
                .thenReturn(Optional.of(cc));
        when(authorizationService.isAdminOrProfesor(2L, 5L)).thenReturn(true);

        when(googleClassroomService.crearEstudiante(
                        org.mockito.Mockito.eq(u),
                        org.mockito.Mockito.eq("c1"),
                        org.mockito.Mockito.anyString()))
                .thenReturn(new ClassroomUserResponse("id1", "userX", "Full", "e@e"));

        CreateStudentRequest req = new CreateStudentRequest("userX", null);

        ResponseEntity<?> resp = controller.createStudent("c1", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createStudentShouldReturn403WhenNotAuthorized() throws Exception {
        Usuario u = new Usuario();
        u.setId(2L);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(u);

        SecurityContext sc = org.mockito.Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        Comunidad comunidad = Comunidad.builder().id(9L).build();
        ComunidadClassroom cc = new ComunidadClassroom();
        cc.setComunidad(comunidad);

        when(comunidadClassroomRepository.findByClassroomCourseId("c2"))
                .thenReturn(Optional.of(cc));
        when(authorizationService.isAdminOrProfesor(2L, 9L)).thenReturn(false);

        CreateStudentRequest req = new CreateStudentRequest("userY", null);

        ResponseEntity<?> resp = controller.createStudent("c2", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createStudentShouldSuccessWithValidDataAndAuth() throws Exception {
        Usuario u = new Usuario();
        u.setId(2L);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(u);

        SecurityContext sc = org.mockito.Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        Comunidad comunidad = Comunidad.builder().id(5L).build();
        ComunidadClassroom cc = new ComunidadClassroom();
        cc.setComunidad(comunidad);

        when(comunidadClassroomRepository.findByClassroomCourseId("c1"))
                .thenReturn(Optional.of(cc));
        when(authorizationService.isAdminOrProfesor(2L, 5L)).thenReturn(true);

        when(googleClassroomService.crearEstudiante(
                        org.mockito.Mockito.eq(u),
                        org.mockito.Mockito.eq("c1"),
                        org.mockito.Mockito.anyString()))
                .thenReturn(new ClassroomUserResponse("id1", "userX", "Full", "e@e"));

        CreateStudentRequest req = new CreateStudentRequest("userX", null);

        ResponseEntity<?> resp = controller.createStudent("c1", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createStudentShouldHandleMultipleAuthorizationChecks() throws Exception {
        Usuario u1 = new Usuario();
        u1.setId(1L);
        Usuario u2 = new Usuario();
        u2.setId(2L);

        Authentication auth1 = org.mockito.Mockito.mock(Authentication.class);
        when(auth1.isAuthenticated()).thenReturn(true);
        when(auth1.getPrincipal()).thenReturn(u1);

        Comunidad comunidad = Comunidad.builder().id(5L).build();
        ComunidadClassroom cc = new ComunidadClassroom();
        cc.setComunidad(comunidad);

        when(comunidadClassroomRepository.findByClassroomCourseId("c1"))
                .thenReturn(Optional.of(cc));
        when(authorizationService.isAdminOrProfesor(1L, 5L)).thenReturn(true);

        when(googleClassroomService.crearEstudiante(
                        org.mockito.Mockito.eq(u1),
                        org.mockito.Mockito.eq("c1"),
                        org.mockito.Mockito.anyString()))
                .thenReturn(new ClassroomUserResponse("id1", "user1", "User 1", "u1@e"));

        CreateStudentRequest req = new CreateStudentRequest("user1", null);

        SecurityContext sc1 = org.mockito.Mockito.mock(SecurityContext.class);
        when(sc1.getAuthentication()).thenReturn(auth1);
        SecurityContextHolder.setContext(sc1);

        ResponseEntity<?> resp = controller.createStudent("c1", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

        @Test
        void authorizeUrlShouldReturnUnauthorizedWhenNoAuthentication() {
                SecurityContextHolder.clearContext();

                ResponseEntity<Map<String, String>> response = controller.authorizeUrl(1L, 2L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(response.getBody()).containsEntry("error", "unauthorized");
        }

        @Test
        void authorizeUrlShouldReturnUnauthorizedWhenPrincipalIsInvalid() {
                setAuthContext("not-a-user", true);

                ResponseEntity<Map<String, String>> response = controller.authorizeUrl(1L, 2L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(response.getBody()).containsEntry("error", "invalid_user");
        }

        @Test
        void authorizeUrlShouldReturnGeneratedUrlForAuthenticatedUser() {
                Usuario u = new Usuario();
                u.setId(33L);
                setAuthContext(u, true);

                when(googleClassroomService.buildAuthorizeUrlForUser(33L, 9L, 8L, false))
                                .thenReturn("https://accounts.google.com/test");

                ResponseEntity<Map<String, String>> response = controller.authorizeUrl(9L, 8L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("url", "https://accounts.google.com/test");
        }

        @Test
        void authorizeShouldReturnRedirectWhenUserIsAuthenticated() {
                Usuario u = new Usuario();
                u.setId(10L);
                setAuthContext(u, true);
                when(googleClassroomService.buildAuthorizeUrlForUser(10L, null, null, false))
                                .thenReturn("https://accounts.google.com/redirect");

                ResponseEntity<Void> response = controller.authorize();

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
                assertThat(response.getHeaders().getLocation().toString())
                                .isEqualTo("https://accounts.google.com/redirect");
        }

        @Test
        void callbackShouldReturnErrorWhenNoCodeIsProvided() throws Exception {
                ResponseEntity<String> response = controller.callback(null, null, "state");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("no_code");
        }

        @Test
        void callbackShouldReturnErrorWhenStateIsInvalid() throws Exception {
                when(googleClassroomService.consumeState("bad-state")).thenReturn(null);

                ResponseEntity<String> response = controller.callback("code", null, "bad-state");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("invalid_state");
        }

        @Test
        void callbackShouldReturnErrorWhenUserFromStateDoesNotExist() throws Exception {
                when(googleClassroomService.consumeState("state-u")).thenReturn(new GoogleClassroomService.OAuthCtx(91L, 1L, 2L));
                when(usuarioRepository.findById(91L)).thenReturn(Optional.empty());

                ResponseEntity<String> response = controller.callback("code", null, "state-u");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("user_not_found:91");
        }

        @Test
        void callbackShouldExchangeTokensAndReturnCoursesPayload() throws Exception {
                RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
                ReflectionTestUtils.setField(controller, "rest", restTemplate);
                ReflectionTestUtils.setField(controller, "clientId", "client-id");
                ReflectionTestUtils.setField(controller, "clientSecret", "client-secret");
                ReflectionTestUtils.setField(controller, "redirectUri", "http://localhost/callback");

                Usuario user = new Usuario();
                user.setId(7L);
                when(googleClassroomService.consumeState("state-ok"))
                                .thenReturn(new GoogleClassroomService.OAuthCtx(7L, 55L, 99L));
                when(usuarioRepository.findById(7L)).thenReturn(Optional.of(user));

                when(restTemplate.postForEntity(
                                                eq("https://oauth2.googleapis.com/token"), any(HttpEntity.class), eq(String.class)))
                                .thenReturn(
                                                ResponseEntity.ok(
                                                                "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}"));
                when(restTemplate.exchange(
                                                eq("https://classroom.googleapis.com/v1/courses"),
                                                eq(HttpMethod.GET),
                                                any(HttpEntity.class),
                                                eq(String.class)))
                                .thenReturn(ResponseEntity.ok("{\"courses\":[]}"));

                ResponseEntity<String> response = controller.callback("code-ok", null, "state-ok");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("window.opener.postMessage");
                assertThat(response.getBody()).contains("communityId:55");
                assertThat(response.getBody()).contains("requestId:99");
                verify(googleClassroomService).guardarConexionOAuth(user, "at", "rt", 3600L);
        }

        @Test
        void callbackShouldReturnInsufficientScopesWhenGoogleDeniesCoursesAccess() throws Exception {
                RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
                ReflectionTestUtils.setField(controller, "rest", restTemplate);
                ReflectionTestUtils.setField(controller, "clientId", "client-id");
                ReflectionTestUtils.setField(controller, "clientSecret", "client-secret");
                ReflectionTestUtils.setField(controller, "redirectUri", "http://localhost/callback");

                Usuario user = new Usuario();
                user.setId(8L);
                when(googleClassroomService.consumeState("state-scope"))
                                .thenReturn(new GoogleClassroomService.OAuthCtx(8L, null, null));
                when(usuarioRepository.findById(8L)).thenReturn(Optional.of(user));

                when(restTemplate.postForEntity(
                                                eq("https://oauth2.googleapis.com/token"), any(HttpEntity.class), eq(String.class)))
                                .thenReturn(
                                                ResponseEntity.ok(
                                                                "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}"));

                HttpClientErrorException forbidden =
                                HttpClientErrorException.create(
                                                HttpStatus.FORBIDDEN,
                                                "Forbidden",
                                                HttpHeaders.EMPTY,
                                                "{}".getBytes(StandardCharsets.UTF_8),
                                                StandardCharsets.UTF_8);
                when(restTemplate.exchange(
                                                eq("https://classroom.googleapis.com/v1/courses"),
                                                eq(HttpMethod.GET),
                                                any(HttpEntity.class),
                                                eq(String.class)))
                                .thenThrow(forbidden);

                ResponseEntity<String> response = controller.callback("code-ok", null, "state-scope");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("insufficient_scopes");
        }

        @Test
        void createTeacherShouldReturnUnauthorizedWhenNoAuthenticatedUser() {
                SecurityContextHolder.clearContext();

                ResponseEntity<?> response =
                                controller.createTeacher("course-1", new CreateTeacherRequest("teacher@example.com"));

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void createTeacherShouldReturnForbiddenWhenCourseIsNotLinked() {
                Usuario u = new Usuario();
                u.setId(2L);
                setAuthContext(u, true);
                when(comunidadClassroomRepository.findByClassroomCourseId("course-x"))
                                .thenReturn(Optional.empty());

                ResponseEntity<?> response =
                                controller.createTeacher("course-x", new CreateTeacherRequest("teacher@example.com"));

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void createTeacherShouldReturnCreatedWhenUserIsAuthorized() {
                Usuario u = new Usuario();
                u.setId(3L);
                setAuthContext(u, true);
                Comunidad comunidad = Comunidad.builder().id(42L).build();
                ComunidadClassroom cc = new ComunidadClassroom();
                cc.setComunidad(comunidad);

                when(comunidadClassroomRepository.findByClassroomCourseId("course-ok"))
                                .thenReturn(Optional.of(cc));
                when(authorizationService.isAdminOrProfesor(3L, 42L)).thenReturn(true);
                when(googleClassroomService.crearProfesor(eq(u), eq("course-ok"), any()))
                                .thenReturn(new ClassroomUserResponse("id", "u", "User", "u@e"));

                ResponseEntity<?> response =
                                controller.createTeacher("course-ok", new CreateTeacherRequest("teacher@example.com"));

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        void listarArchivosCursoVinculadoShouldReturnForbiddenForNonAdminOrProfesor() {
                Usuario u = new Usuario();
                u.setId(5L);
                setAuthContext(u, true);
                when(authorizationService.isAdminOrProfesor(5L, 77L)).thenReturn(false);

                ResponseEntity<?> response = controller.listarArchivosCursoVinculado(77L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void listarArchivosCursoVinculadoShouldReturnDataWhenAuthorized() {
                Usuario u = new Usuario();
                u.setId(6L);
                setAuthContext(u, true);
                when(authorizationService.isAdminOrProfesor(6L, 88L)).thenReturn(true);
                when(googleClassroomService.listarArchivosCursoVinculado(u, 88L))
                                .thenReturn(Map.of("materials", java.util.List.of(), "courseWork", java.util.List.of()));

                ResponseEntity<?> response = controller.listarArchivosCursoVinculado(88L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void listarArchivosCursoVinculadoShouldPropagateGoogleHttpStatus() {
                Usuario u = new Usuario();
                u.setId(6L);
                setAuthContext(u, true);
                when(authorizationService.isAdminOrProfesor(6L, 89L)).thenReturn(true);

                HttpClientErrorException forbidden =
                                HttpClientErrorException.create(
                                                HttpStatus.FORBIDDEN,
                                                "Forbidden",
                                                HttpHeaders.EMPTY,
                                                "{}".getBytes(StandardCharsets.UTF_8),
                                                StandardCharsets.UTF_8);
                when(googleClassroomService.listarArchivosCursoVinculado(u, 89L)).thenThrow(forbidden);

                ResponseEntity<?> response = controller.listarArchivosCursoVinculado(89L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void obtenerEstadisticasAlumnosShouldReturnUnauthorizedWhenPrincipalIsInvalid() {
                setAuthContext("bad-principal", true);

                ResponseEntity<?> response = controller.obtenerEstadisticasAlumnos(1L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void obtenerEstadisticasAlumnosShouldReturnDataWhenAuthorized() {
                Usuario u = new Usuario();
                u.setId(7L);
                setAuthContext(u, true);
                when(authorizationService.isAdminOrProfesor(7L, 100L)).thenReturn(true);
                when(googleClassroomService.obtenerEstadisticasAlumnos(u, 100L))
                                .thenReturn(Map.of("total", 14, "activos", 10));

                ResponseEntity<?> response = controller.obtenerEstadisticasAlumnos(100L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void obtenerEstadisticasAlumnosShouldReturnServerErrorWhenServiceFails() {
                Usuario u = new Usuario();
                u.setId(7L);
                setAuthContext(u, true);
                when(authorizationService.isAdminOrProfesor(7L, 101L)).thenReturn(true);
                when(googleClassroomService.obtenerEstadisticasAlumnos(u, 101L))
                                .thenThrow(new RuntimeException("boom"));

                ResponseEntity<?> response = controller.obtenerEstadisticasAlumnos(101L);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        private void setAuthContext(Object principal, boolean authenticated) {
                Authentication auth = org.mockito.Mockito.mock(Authentication.class);
                lenient().when(auth.isAuthenticated()).thenReturn(authenticated);
                lenient().when(auth.getName()).thenReturn("test-user");
                when(auth.getPrincipal()).thenReturn(principal);

                SecurityContext sc = org.mockito.Mockito.mock(SecurityContext.class);
                when(sc.getAuthentication()).thenReturn(auth);
                SecurityContextHolder.setContext(sc);
        }
}
