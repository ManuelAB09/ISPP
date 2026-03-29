package es.us.meerkat.backend.service.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
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

import es.us.meerkat.backend.dto.recommendations.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.dto.recommendations.RegistrarActividadRequest;
import es.us.meerkat.backend.dto.recommendations.ValoracionTutorRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.NivelDificultad;
import es.us.meerkat.backend.entity.google.Contenido;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.recommendations.FactorRecomendacion;
import es.us.meerkat.backend.entity.recommendations.Recomendacion;
import es.us.meerkat.backend.entity.recommendations.TipoRecomendacion;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.tutors.ValoracionTutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioRepository;
import es.us.meerkat.backend.repository.recommendations.ContenidoRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRecomendacionRepository;
import es.us.meerkat.backend.repository.recommendations.RecomendacionRepository;
import es.us.meerkat.backend.repository.recommendations.ValoracionTutorRepository;
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
    @Mock private ValoracionTutorRepository valoracionRepository;
    @Mock private ContenidoRepository contenidoRepository;
    @Mock private CuestionarioRepository cuestionarioRepository;

    @InjectMocks private RecommendationService recommendationService;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("User " + id);
        u.setIntereses(List.of("java", "spring"));
        return u;
    }

    private Recomendacion buildRecomendacion(Long id, Long usuarioId, TipoRecomendacion tipo) {
        Usuario u = new Usuario();
        u.setId(usuarioId);
        return Recomendacion.builder()
                .id(id)
                .usuario(u)
                .tipo(tipo)
                .idObjetoRecomendado(10L)
                .titulo("Rec " + id)
                .puntuacionRelevancia(75.0)
                .vista(false)
                .build();
    }

    // ================================================================
    // registrarActividad
    // ================================================================

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
    void registrarActividadWithRequestShouldSaveActivity() {
        Usuario usuario = buildUsuario(1L);
        RegistrarActividadRequest request =
                RegistrarActividadRequest.builder()
                        .tipoActividad("VIEW")
                        .categoriaObjeto("CONTENIDO")
                        .idObjeto(5L)
                        .terminosBusqueda("python")
                        .duracionSegundos(120L)
                        .datosAdicionales("{}")
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        recommendationService.registrarActividad(1L, request);

        verify(actividadRepository).save(any());
    }

    @Test
    void registrarActividadShouldThrowWhenUsuarioNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                recommendationService.registrarActividad(
                                        99L, "VIEW", "PROFESOR", 1L, "java"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ================================================================
    // generarRecomendacionesUsuario
    // ================================================================

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

    // ================================================================
    // marcarComoVista
    // ================================================================

    @Test
    void marcarComoVistaShouldUpdateRecomendacion() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.PROFESOR);

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));

        recommendationService.marcarComoVista(1L, 5L);

        assertThat(rec.getVista()).isTrue();
        assertThat(rec.getFechaVista()).isNotNull();
        verify(recomendacionRepository).save(rec);
    }

    @Test
    void marcarComoVistaShouldThrowWhenNotOwned() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.PROFESOR);

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recommendationService.marcarComoVista(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permisos");
    }

    @Test
    void marcarComoVistaShouldThrowWhenNotFound() {
        when(recomendacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.marcarComoVista(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    // ================================================================
    // eliminarRecomendacion
    // ================================================================

    @Test
    void eliminarRecomendacionShouldDeleteWhenOwned() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.CONTENIDO);

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));

        recommendationService.eliminarRecomendacion(1L, 5L);

        verify(recomendacionRepository).delete(rec);
    }

    @Test
    void eliminarRecomendacionShouldThrowWhenNotOwned() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.CONTENIDO);

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recommendationService.eliminarRecomendacion(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarRecomendacionShouldThrowWhenNotFound() {
        when(recomendacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.eliminarRecomendacion(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // darFeedbackRecomendacion
    // ================================================================

    @Test
    void darFeedbackShouldThrowWhenNotOwned() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.PROFESOR);
        FeedbackRecomendacionRequest request =
                FeedbackRecomendacionRequest.builder().esUtil(true).build();

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> recommendationService.darFeedbackRecomendacion(1L, request, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permisos");
    }

    @Test
    void darFeedbackNegativeShouldRegenerateTipo() {
        Usuario usuario = buildUsuario(1L);
        Recomendacion rec = buildRecomendacion(1L, 1L, TipoRecomendacion.PROFESOR);
        FeedbackRecomendacionRequest request =
                FeedbackRecomendacionRequest.builder()
                        .esUtil(false)
                        .comentario("No relevante")
                        .satisfaccion(2)
                        .build();

        when(recomendacionRepository.findById(1L)).thenReturn(Optional.of(rec));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(feedbackRepository.findByRecomendacionIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        recommendationService.darFeedbackRecomendacion(1L, request, 1L);

        verify(feedbackRepository).save(any());
        assertThat(rec.getEsFavorable()).isFalse();
        // Negative feedback triggers regeneration
        verify(recomendacionRepository).deleteByUsuarioIdAndTipo(1L, TipoRecomendacion.PROFESOR);
    }

    // ================================================================
    // valorarTutor
    // ================================================================

    @Test
    void valorarTutorShouldSaveValoracion() {
        Usuario tutor_user = buildUsuario(10L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutor_user);
        Usuario usuario = buildUsuario(5L);
        ValoracionTutorRequest request =
                ValoracionTutorRequest.builder().puntuacion(4).comentario("Buen tutor").build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(valoracionRepository.findByTutorIdAndUsuarioId(1L, 5L)).thenReturn(Optional.empty());

        recommendationService.valorarTutor(1L, 5L, request);

        verify(valoracionRepository).save(any(ValoracionTutor.class));
    }

    @Test
    void valorarTutorShouldThrowWhenSelfRating() {
        Usuario tutor_user = buildUsuario(5L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutor_user);
        ValoracionTutorRequest request = ValoracionTutorRequest.builder().puntuacion(5).build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> recommendationService.valorarTutor(1L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes valorarte a ti mismo");
    }

    @Test
    void valorarTutorShouldUpdateExistingValoracion() {
        Usuario tutor_user = buildUsuario(10L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutor_user);
        Usuario usuario = buildUsuario(5L);
        ValoracionTutor existing =
                ValoracionTutor.builder()
                        .id(50L)
                        .tutor(tutor)
                        .usuario(usuario)
                        .puntuacion(3)
                        .build();
        ValoracionTutorRequest request =
                ValoracionTutorRequest.builder().puntuacion(5).comentario("Mejorado").build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(valoracionRepository.findByTutorIdAndUsuarioId(1L, 5L))
                .thenReturn(Optional.of(existing));

        recommendationService.valorarTutor(1L, 5L, request);

        verify(valoracionRepository).save(existing);
        assertThat(existing.getPuntuacion()).isEqualTo(5);
    }

    @Test
    void valorarTutorShouldThrowWhenTutorNotFound() {
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                recommendationService.valorarTutor(
                                        99L,
                                        1L,
                                        ValoracionTutorRequest.builder().puntuacion(4).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // getValoracionMedia
    // ================================================================

    @Test
    void getValoracionMediaShouldReturnAverage() {
        when(valoracionRepository.findMediaByTutorId(1L)).thenReturn(4.5);

        Double media = recommendationService.getValoracionMedia(1L);

        assertThat(media).isEqualTo(4.5);
    }

    @Test
    void getValoracionMediaShouldReturnNullWhenNoRatings() {
        when(valoracionRepository.findMediaByTutorId(99L)).thenReturn(null);

        Double media = recommendationService.getValoracionMedia(99L);

        assertThat(media).isNull();
    }

    // ================================================================
    // getRecomendacionesProfesores (paginated)
    // ================================================================

    @Test
    void getRecomendacionesProfesoresShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.PROFESOR);
        Page<Recomendacion> page = new PageImpl<>(List.of(rec));

        when(recomendacionRepository.findPorTipo(5L, TipoRecomendacion.PROFESOR, pageable))
                .thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesProfesores(5L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getRecomendacionesContenidoShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recomendacion> page = new PageImpl<>(List.of());

        when(recomendacionRepository.findPorTipo(5L, TipoRecomendacion.CONTENIDO, pageable))
                .thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesContenido(5L, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getRecomendacionesCuestionariosShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recomendacion> page = new PageImpl<>(List.of());

        when(recomendacionRepository.findPorTipo(5L, TipoRecomendacion.CUESTIONARIO, pageable))
                .thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesCuestionarios(5L, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getRecomendacionesComunidadesShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recomendacion> page = new PageImpl<>(List.of());

        when(recomendacionRepository.findPorTipo(5L, TipoRecomendacion.COMUNIDAD, pageable))
                .thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesComunidades(5L, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getRecomendacionesActivasShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recomendacion> page = new PageImpl<>(List.of());

        when(recomendacionRepository.findRecomendacionesActivas(5L, pageable)).thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesActivas(5L, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getRecomendacionesNoVistasShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recomendacion> page = new PageImpl<>(List.of());

        when(recomendacionRepository.findNoVistas(5L, pageable)).thenReturn(page);

        Page<?> result = recommendationService.getRecomendacionesNoVistas(5L, pageable);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // motivoFactor
    // ================================================================

    @Test
    void motivoFactorShouldReturnCorrectMessages() {
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.INTERES_SIMILAR))
                .isEqualTo("Coincide con tus intereses");
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.UBICACION))
                .isEqualTo("Popular en tu zona");
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.NIVEL_EDUCATIVO))
                .isEqualTo("Acorde a tu nivel de estudios");
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.COMUNIDAD_SIMILAR))
                .isEqualTo("Similar a comunidades donde participas");
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.POPULARIDAD))
                .isEqualTo("Tendencia actual");
        assertThat(recommendationService.motivoFactor(FactorRecomendacion.ACTIVIDAD_SIMILAR))
                .isEqualTo("Basado en tu actividad reciente");
    }

    // ================================================================
    // getRecomendacionesPage
    // ================================================================

    @Test
    void getRecomendacionesPageShouldReturnStructuredResponse() {
        Recomendacion rec = buildRecomendacion(1L, 5L, TipoRecomendacion.PROFESOR);

        when(recomendacionRepository.findTopParaTi(eq(5L), eq(6))).thenReturn(List.of(rec));
        when(recomendacionRepository.findPorTipo(eq(5L), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        var result = recommendationService.getRecomendacionesPage(5L);

        assertThat(result).isNotNull();
        assertThat(result.getGeneradoEn()).isNotNull();
    }

    // ================================================================
    // regenerarTipo
    // ================================================================

    @Test
    void regenerarTipoShouldDeleteAndRegenerateForProfesor() {
        Usuario usuario = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        recommendationService.regenerarTipo(1L, TipoRecomendacion.PROFESOR);

        verify(recomendacionRepository).deleteByUsuarioIdAndTipo(1L, TipoRecomendacion.PROFESOR);
    }

    @Test
    void regenerarTipoShouldHandleUsuarioNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        // Should not throw — exception is caught internally
        recommendationService.regenerarTipo(99L, TipoRecomendacion.PROFESOR);
    }

    // ================================================================
    // generarRecomendacionesUsuario — comprehensive coverage
    // ================================================================

    @Test
    void generarRecomendacionesUsuarioShouldGenerateProfesoresWhenTutorsExist() {
        Usuario usuario = buildUsuario(1L);
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre("Sevilla");
        usuario.setUbicacion(ubicacion);
        usuario.setNivelEstudios("Grado en Ingeniería Informática");

        // Tutor with matching especialidades
        Usuario tutorUser = new Usuario();
        tutorUser.setId(100L);
        tutorUser.setNombre("Tutor Expert");
        tutorUser.setFoto("foto.png");
        Tutor tutor = new Tutor();
        tutor.setId(50L);
        tutor.setUsuario(tutorUser);
        tutor.setEspecialidades(List.of("java", "spring"));
        tutor.setBio("Experto en ingeniería de software");
        Ubicacion tutorUbic = new Ubicacion();
        tutorUbic.setNombre("Sevilla");
        tutor.setUbicacion(tutorUbic);
        tutor.setVerificado(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of("java"));
        when(tutorRepository.findByVerificadoTrue()).thenReturn(List.of(tutor));
        when(valoracionRepository.countByTutorId(50L)).thenReturn(5L);
        when(valoracionRepository.findMediaByTutorId(50L)).thenReturn(4.5);
        when(recomendacionRepository.findByUsuarioTipoObjeto(
                        eq(1L), eq(TipoRecomendacion.PROFESOR), eq(50L)))
                .thenReturn(Collections.emptyList());
        // Comunidades, contenido, quizzes empty
        when(comunidadRepository.findAll()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        recommendationService.generarRecomendacionesUsuario(1L);

        verify(recomendacionRepository, atLeastOnce()).save(any(Recomendacion.class));
    }

    @Test
    void generarRecomendacionesUsuarioShouldGenerateComunidadesWhenPublicExist() {
        Usuario usuario = buildUsuario(2L);
        usuario.setUbicacion(null);

        Comunidad comunidad =
                Comunidad.builder()
                        .id(10L)
                        .nombre("Java devs Sevilla")
                        .descripcion("Comunidad de desarrolladores Java")
                        .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                        .build();

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(2L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(tutorRepository.findByVerificadoTrue()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(comunidadRepository.findAll()).thenReturn(List.of(comunidad));
        when(miembroRepository.countByComunidadId(10L)).thenReturn(200L);
        when(recomendacionRepository.findByUsuarioTipoObjeto(
                        eq(2L), eq(TipoRecomendacion.COMUNIDAD), eq(10L)))
                .thenReturn(Collections.emptyList());

        recommendationService.generarRecomendacionesUsuario(2L);

        verify(recomendacionRepository, atLeastOnce()).save(any(Recomendacion.class));
    }

    @Test
    void generarRecomendacionesUsuarioShouldGenerateContenidoWhenTemasExist() {
        Usuario usuario = buildUsuario(3L);
        usuario.setNivelEstudios("Grado");

        Contenido contenido =
                Contenido.builder()
                        .id(20L)
                        .titulo("Tutorial Spring Boot")
                        .descripcion("Intro a Spring")
                        .imagenUrl("img.png")
                        .materia("spring")
                        .build();

        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(3L), any(LocalDateTime.class)))
                .thenReturn(List.of("spring"));
        when(tutorRepository.findByVerificadoTrue()).thenReturn(Collections.emptyList());
        when(comunidadRepository.findAll()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(contenidoRepository.findActivosByTemasYNivel(anyList(), anyString()))
                .thenReturn(List.of(contenido));
        when(actividadRepository.findObjetosVisitados(eq(3L), eq("Contenido")))
                .thenReturn(Collections.emptyList());
        when(recomendacionRepository.findByUsuarioTipoObjeto(
                        eq(3L), eq(TipoRecomendacion.CONTENIDO), eq(20L)))
                .thenReturn(Collections.emptyList());

        recommendationService.generarRecomendacionesUsuario(3L);

        verify(recomendacionRepository, atLeastOnce()).save(any(Recomendacion.class));
    }

    @Test
    void generarRecomendacionesUsuarioShouldGenerateContenidoWithoutNivel() {
        Usuario usuario = buildUsuario(4L);
        usuario.setNivelEstudios(null); // blank nivel → use findActivosByTemasInteres

        Contenido contenido =
                Contenido.builder()
                        .id(21L)
                        .titulo("Python Basics")
                        .descripcion("Intro")
                        .imagenUrl(null)
                        .materia("python")
                        .build();

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(4L), any(LocalDateTime.class)))
                .thenReturn(List.of("python"));
        when(tutorRepository.findByVerificadoTrue()).thenReturn(Collections.emptyList());
        when(comunidadRepository.findAll()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(4L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(contenidoRepository.findActivosByTemasInteres(anyList()))
                .thenReturn(List.of(contenido));
        when(actividadRepository.findObjetosVisitados(eq(4L), eq("Contenido")))
                .thenReturn(Collections.emptyList());
        when(recomendacionRepository.findByUsuarioTipoObjeto(
                        eq(4L), eq(TipoRecomendacion.CONTENIDO), eq(21L)))
                .thenReturn(Collections.emptyList());

        recommendationService.generarRecomendacionesUsuario(4L);

        verify(contenidoRepository).findActivosByTemasInteres(anyList());
    }

    @Test
    void generarRecomendacionesUsuarioShouldGenerateQuizzesWithWeakTopics() {
        Usuario usuario = buildUsuario(5L);

        Cuestionario quiz =
                Cuestionario.builder()
                        .id(30L)
                        .titulo("Java Quiz")
                        .descripcion("Test your Java")
                        .imagenUrl(null)
                        .materia("java")
                        .dificultad(NivelDificultad.BASICO)
                        .build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(5L), any(LocalDateTime.class)))
                .thenReturn(List.of("java"));
        when(tutorRepository.findByVerificadoTrue()).thenReturn(Collections.emptyList());
        when(comunidadRepository.findAll()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        // No contenido
        when(contenidoRepository.findActivosByTemasInteres(anyList()))
                .thenReturn(Collections.emptyList());
        when(actividadRepository.findObjetosVisitados(eq(5L), eq("Contenido")))
                .thenReturn(Collections.emptyList());
        // Rendimiento bajo en java → weak topic
        when(actividadRepository.findRendimientoQuizPorTema(5L))
                .thenReturn(List.<Object[]>of(new Object[] {"java", 0.3}));
        // Quizzes - weak topic (score < 0.5 in rendimiento)
        when(cuestionarioRepository.findActivosByTemasYDificultad(
                        anyList(), eq(NivelDificultad.BASICO)))
                .thenReturn(List.of(quiz));
        when(cuestionarioRepository.findActivosByTemasYDificultad(
                        anyList(), eq(NivelDificultad.INTERMEDIO)))
                .thenReturn(Collections.emptyList());
        when(actividadRepository.findObjetosVisitados(eq(5L), eq("Cuestionario")))
                .thenReturn(Collections.emptyList());
        when(recomendacionRepository.findByUsuarioTipoObjeto(
                        eq(5L), eq(TipoRecomendacion.CUESTIONARIO), eq(30L)))
                .thenReturn(Collections.emptyList());

        recommendationService.generarRecomendacionesUsuario(5L);

        verify(recomendacionRepository, atLeastOnce()).save(any(Recomendacion.class));
    }

    @Test
    void generarRecomendacionesUsuarioShouldSkipSelfAsProfesor() {
        Usuario usuario = buildUsuario(1L);

        // Tutor whose user is the same as the requesting user
        Tutor selfTutor = new Tutor();
        selfTutor.setId(50L);
        selfTutor.setUsuario(usuario); // same user
        selfTutor.setEspecialidades(List.of("java"));
        selfTutor.setVerificado(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(tutorRepository.findByVerificadoTrue()).thenReturn(List.of(selfTutor));
        when(comunidadRepository.findAll()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        recommendationService.generarRecomendacionesUsuario(1L);

        // Self-tutor should be filtered out → no PROFESOR recommendation saved
        verify(recomendacionRepository, never()).save(any(Recomendacion.class));
    }

    @Test
    void generarRecomendacionesUsuarioShouldSkipAlreadyJoinedCommunity() {
        Usuario usuario = buildUsuario(6L);

        Comunidad comunidad =
                Comunidad.builder()
                        .id(10L)
                        .nombre("Java devs")
                        .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                        .build();

        MiembroComunidad miembro = new MiembroComunidad();
        miembro.setComunidad(comunidad);

        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario));
        when(actividadRepository.findTemasInteres(eq(6L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(tutorRepository.findByVerificadoTrue()).thenReturn(Collections.emptyList());
        when(miembroRepository.findByUsuarioId(eq(6L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(miembro)));
        when(comunidadRepository.findAll()).thenReturn(List.of(comunidad));

        recommendationService.generarRecomendacionesUsuario(6L);

        // Already joined → no community recommendation
        verify(recomendacionRepository, never()).save(any(Recomendacion.class));
    }
}
