package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import es.us.meerkat.backend.controller.google.GoogleClassroomController;
import es.us.meerkat.backend.dto.ClassroomUserResponse;
import es.us.meerkat.backend.dto.CreateStudentRequest;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
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
}
