package es.us.meerkat.backend.service.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.recommendations.Feedback;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private FeedbackService feedbackService;

    // ================================================================
    // createFeedback
    // ================================================================

    @Test
    void createFeedbackShouldSaveWhenProfesorHasCorrectRole() {
        Usuario profesor =
                Usuario.builder().id(1L).nombre("Prof").email("p@t.com").password("p").build();
        Usuario alumno =
                Usuario.builder().id(2L).nombre("Al").email("a@t.com").password("p").build();
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(profesor));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alumno));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(authorizationService.getUserRoleInCommunity(1L, 10L))
                .thenReturn(RolComunidad.PROFESOR);
        when(authorizationService.isMemberOf(2L, 10L)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(
                        inv -> {
                            Feedback f = inv.getArgument(0);
                            f.setId(100L);
                            return f;
                        });

        Feedback result = feedbackService.createFeedback(1L, 2L, 10L, "Buen trabajo", 5);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getProfesor()).isEqualTo(profesor);
        assertThat(result.getAlumno()).isEqualTo(alumno);
        assertThat(result.getComunidad()).isEqualTo(comunidad);
        assertThat(result.getContenido()).isEqualTo("Buen trabajo");
        assertThat(result.getCalificacion()).isEqualTo(5);
    }

    @Test
    void createFeedbackShouldSucceedWhenUserIsAdmin() {
        Usuario admin =
                Usuario.builder().id(1L).nombre("Admin").email("adm@t.com").password("p").build();
        Usuario alumno =
                Usuario.builder().id(2L).nombre("Al").email("a@t.com").password("p").build();
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alumno));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(authorizationService.getUserRoleInCommunity(1L, 10L)).thenReturn(RolComunidad.ADMIN);
        when(authorizationService.isMemberOf(2L, 10L)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        Feedback result = feedbackService.createFeedback(1L, 2L, 10L, "Ok", 3);

        assertThat(result.getProfesor()).isEqualTo(admin);
    }

    @Test
    void createFeedbackShouldThrowWhenProfesorNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createFeedback(99L, 2L, 10L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profesor no encontrado");
    }

    @Test
    void createFeedbackShouldThrowWhenAlumnoNotFound() {
        when(usuarioRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("P")
                                        .email("p@t.com")
                                        .password("p")
                                        .build()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createFeedback(1L, 2L, 10L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alumno no encontrado");
    }

    @Test
    void createFeedbackShouldThrowWhenComunidadNotFound() {
        when(usuarioRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("P")
                                        .email("p@t.com")
                                        .password("p")
                                        .build()));
        when(usuarioRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(2L)
                                        .nombre("A")
                                        .email("a@t.com")
                                        .password("p")
                                        .build()));
        when(comunidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createFeedback(1L, 2L, 99L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comunidad no encontrada");
    }

    @Test
    void createFeedbackShouldThrowWhenUserIsNotProfesorNorAdmin() {
        when(usuarioRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("P")
                                        .email("p@t.com")
                                        .password("p")
                                        .build()));
        when(usuarioRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(2L)
                                        .nombre("A")
                                        .email("a@t.com")
                                        .password("p")
                                        .build()));
        when(comunidadRepository.findById(10L))
                .thenReturn(Optional.of(Comunidad.builder().id(10L).nombre("C").build()));
        when(authorizationService.getUserRoleInCommunity(1L, 10L)).thenReturn(RolComunidad.ALUMNO);

        assertThatThrownBy(() -> feedbackService.createFeedback(1L, 2L, 10L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo profesores o administradores");
    }

    @Test
    void createFeedbackShouldThrowWhenUserRoleIsNull() {
        when(usuarioRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("P")
                                        .email("p@t.com")
                                        .password("p")
                                        .build()));
        when(usuarioRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(2L)
                                        .nombre("A")
                                        .email("a@t.com")
                                        .password("p")
                                        .build()));
        when(comunidadRepository.findById(10L))
                .thenReturn(Optional.of(Comunidad.builder().id(10L).nombre("C").build()));
        when(authorizationService.getUserRoleInCommunity(1L, 10L)).thenReturn(null);

        assertThatThrownBy(() -> feedbackService.createFeedback(1L, 2L, 10L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo profesores o administradores");
    }

    @Test
    void createFeedbackShouldThrowWhenAlumnoIsNotCommunityMember() {
        when(usuarioRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(1L)
                                        .nombre("P")
                                        .email("p@t.com")
                                        .password("p")
                                        .build()));
        when(usuarioRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                Usuario.builder()
                                        .id(2L)
                                        .nombre("A")
                                        .email("a@t.com")
                                        .password("p")
                                        .build()));
        when(comunidadRepository.findById(10L))
                .thenReturn(Optional.of(Comunidad.builder().id(10L).nombre("C").build()));
        when(authorizationService.getUserRoleInCommunity(1L, 10L))
                .thenReturn(RolComunidad.PROFESOR);
        when(authorizationService.isMemberOf(2L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> feedbackService.createFeedback(1L, 2L, 10L, "X", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El alumno no es miembro");
    }

    // ================================================================
    // listFeedbacksByCommunity
    // ================================================================

    @Test
    void listFeedbacksByCommunityShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Feedback fb = Feedback.builder().id(1L).contenido("C").build();
        Page<Feedback> page = new PageImpl<>(List.of(fb));

        when(feedbackRepository.findByComunidadId(10L, pageable)).thenReturn(page);

        Page<Feedback> result = feedbackService.listFeedbacksByCommunity(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContenido()).isEqualTo("C");
    }

    // ================================================================
    // listFeedbacksForStudent
    // ================================================================

    @Test
    void listFeedbacksForStudentShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Feedback> emptyPage = new PageImpl<>(List.of());

        when(feedbackRepository.findByAlumnoId(2L, pageable)).thenReturn(emptyPage);

        Page<Feedback> result = feedbackService.listFeedbacksForStudent(2L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
