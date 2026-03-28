package es.us.meerkat.backend.service.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.recommendations.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.entity.recommendations.Recomendacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRecomendacionRepository;
import es.us.meerkat.backend.repository.recommendations.RecomendacionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.ActividadUsuarioRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private RecomendacionRepository recomendacionRepository;

    @Mock private FeedbackRecomendacionRepository feedbackRepository;

    @Mock private ActividadUsuarioRepository actividadRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private TutorRepository tutorRepository;

    @Mock private ComunidadRepository comunidadRepository;

    @Mock private MiembroComunidadRepository miembroRepository;

    @InjectMocks private RecommendationService recommendationService;

    @Test
    void registrarActividadShouldSaveUserActivity() {
        Long usuarioId = 1L;
        String tipo = "VIEW";
        String categoria = "PROFESOR";
        Long idObjeto = 5L;
        String terminos = "java";

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        recommendationService.registrarActividad(usuarioId, tipo, categoria, idObjeto, terminos);

        verify(actividadRepository).save(any());
    }

    @Test
    void generarRecomendacionesUsuarioShouldProcessUser() {
        Long usuarioId = 1L;

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setIntereses(List.of("java", "spring"));

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        recommendationService.generarRecomendacionesUsuario(usuarioId);

        verify(usuarioRepository).findById(usuarioId);
    }

    @Test
    void darFeedbackRecomendacionShouldSaveUserFeedback() {
        Long usuarioId = 1L;
        FeedbackRecomendacionRequest request = new FeedbackRecomendacionRequest();
        request.setEsUtil(true);
        request.setComentario("Muy útil");

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Recomendacion recomendacion = new Recomendacion();
        recomendacion.setId(1L);
        recomendacion.setUsuario(usuario);
        recomendacion.setEsFavorable(false);

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(recomendacion));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        recommendationService.darFeedbackRecomendacion(1L, request, usuarioId);

        verify(feedbackRepository).save(any());
    }

    @Test
    void recommendationServiceShouldBeInstantiated() {
        assertThat(recommendationService).isNotNull();
    }
}
