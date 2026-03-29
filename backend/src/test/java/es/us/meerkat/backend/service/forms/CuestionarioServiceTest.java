package es.us.meerkat.backend.service.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.forms.CreateCuestionarioRequest;
import es.us.meerkat.backend.dto.forms.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.CuestionarioIntento;
import es.us.meerkat.backend.entity.forms.NivelDificultad;
import es.us.meerkat.backend.entity.forms.Opcion;
import es.us.meerkat.backend.entity.forms.Pregunta;
import es.us.meerkat.backend.entity.recommendations.TipoPregunta;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioIntentoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CuestionarioServiceTest {

    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CuestionarioIntentoRepository intentoRepository;

    @InjectMocks private CuestionarioService cuestionarioService;

    // ================================================================
    // createCuestionario
    // ================================================================

    @Test
    void createCuestionarioShouldLinkPreguntasAndOpcionesAndSetNumPreguntas() {
        Opcion opcion = Opcion.builder().texto("Opción A").build();
        Pregunta pregunta =
                Pregunta.builder()
                        .enunciado("¿Cuánto es 2+2?")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(opcion)))
                        .build();
        Cuestionario cuestionario =
                Cuestionario.builder()
                        .titulo("Math Quiz")
                        .materia("Matemáticas")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createCuestionario(cuestionario);

        assertThat(result.getNumPreguntas()).isEqualTo(1);
        assertThat(result.getPreguntas().get(0).getCuestionario()).isEqualTo(cuestionario);
        assertThat(result.getPreguntas().get(0).getOpciones().get(0).getPregunta())
                .isEqualTo(pregunta);
    }

    @Test
    void createCuestionarioShouldHandleNullPreguntas() {
        Cuestionario cuestionario =
                Cuestionario.builder()
                        .titulo("Empty Quiz")
                        .materia("General")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(null)
                        .build();

        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createCuestionario(cuestionario);

        assertThat(result.getPreguntas()).isNull();
        verify(cuestionarioRepository).save(cuestionario);
    }

    @Test
    void createCuestionarioShouldHandlePreguntaWithNullOpciones() {
        Pregunta pregunta =
                Pregunta.builder()
                        .enunciado("Pregunta sin opciones")
                        .tipo(TipoPregunta.RESPUESTA_CORTA)
                        .opciones(null)
                        .build();
        Cuestionario cuestionario =
                Cuestionario.builder()
                        .titulo("Quiz")
                        .materia("Ciencias")
                        .dificultad(NivelDificultad.INTERMEDIO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createCuestionario(cuestionario);

        assertThat(result.getPreguntas().get(0).getCuestionario()).isEqualTo(cuestionario);
    }

    // ================================================================
    // createFromDto
    // ================================================================

    @Test
    void createFromDtoShouldCreateCuestionarioWithAllFieldsFromDto() {
        Usuario creador =
                Usuario.builder()
                        .id(1L)
                        .nombre("Profe")
                        .email("p@test.com")
                        .password("pass")
                        .build();

        CreateCuestionarioRequest.OpcionRequest opReq =
                CreateCuestionarioRequest.OpcionRequest.builder()
                        .texto("Verdadero")
                        .orden(1)
                        .correcta(true)
                        .build();
        CreateCuestionarioRequest.PreguntaRequest pregReq =
                CreateCuestionarioRequest.PreguntaRequest.builder()
                        .enunciado("¿Es 2+2=4?")
                        .tipo(TipoPregunta.VERDADERO_FALSO)
                        .opciones(List.of(opReq))
                        .respuestasAceptables(null)
                        .build();
        CreateCuestionarioRequest dto =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .descripcion("Desc")
                        .imagenUrl("http://img.png")
                        .materia("Matemáticas")
                        .tags(List.of("tag1"))
                        .dificultad(NivelDificultad.AVANZADO)
                        .nivelEducativo("Universidad")
                        .numPreguntas(1)
                        .tiempoEstimadoMinutos(10)
                        .activo(true)
                        .publicado(true)
                        .preguntas(List.of(pregReq))
                        .comunidadesIds(List.of(10L))
                        .alumnosIds(List.of(20L))
                        .alumnosEmails(List.of("alumno@test.com"))
                        .build();

        Comunidad comunidad = Comunidad.builder().id(10L).nombre("Com").build();
        Usuario alumnoById =
                Usuario.builder().id(20L).nombre("Al1").email("a1@test.com").password("p").build();
        Usuario alumnoByEmail =
                Usuario.builder()
                        .id(30L)
                        .nombre("Al2")
                        .email("alumno@test.com")
                        .password("p")
                        .build();

        when(comunidadRepository.findAllById(List.of(10L))).thenReturn(List.of(comunidad));
        when(usuarioRepository.findAllById(List.of(20L))).thenReturn(List.of(alumnoById));
        when(usuarioRepository.findByEmail("alumno@test.com"))
                .thenReturn(Optional.of(alumnoByEmail));
        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createFromDto(dto, creador);

        assertThat(result.getTitulo()).isEqualTo("Test Quiz");
        assertThat(result.getCreador()).isEqualTo(creador);
        assertThat(result.getPreguntas()).hasSize(1);
        assertThat(result.getNumPreguntas()).isEqualTo(1);
        assertThat(result.getComunidades()).contains(comunidad);
        assertThat(result.getAlumnos()).contains(alumnoById, alumnoByEmail);
        assertThat(result.getPublicado()).isTrue();
    }

    @Test
    void createFromDtoShouldUseDefaultsWhenOptionalFieldsAreNull() {
        Usuario creador =
                Usuario.builder()
                        .id(1L)
                        .nombre("Profe")
                        .email("p@test.com")
                        .password("pass")
                        .build();

        CreateCuestionarioRequest dto =
                CreateCuestionarioRequest.builder()
                        .titulo("Minimal Quiz")
                        .materia("Lengua")
                        .dificultad(NivelDificultad.BASICO)
                        .tags(null)
                        .numPreguntas(null)
                        .activo(null)
                        .publicado(null)
                        .preguntas(null)
                        .comunidadesIds(null)
                        .alumnosIds(null)
                        .alumnosEmails(null)
                        .build();

        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createFromDto(dto, creador);

        assertThat(result.getTags()).isEmpty();
        assertThat(result.getNumPreguntas()).isEqualTo(0);
        assertThat(result.getActivo()).isTrue();
        assertThat(result.getPublicado()).isFalse();
        assertThat(result.getPreguntas()).isEmpty();
    }

    @Test
    void createFromDtoShouldIgnoreBlankAndNullEmails() {
        Usuario creador =
                Usuario.builder()
                        .id(1L)
                        .nombre("Profe")
                        .email("p@test.com")
                        .password("pass")
                        .build();

        CreateCuestionarioRequest dto =
                CreateCuestionarioRequest.builder()
                        .titulo("Quiz")
                        .materia("Historia")
                        .dificultad(NivelDificultad.INTERMEDIO)
                        .alumnosEmails(List.of("", "  ", "valid@test.com"))
                        .build();

        Usuario found =
                Usuario.builder().id(5L).email("valid@test.com").nombre("V").password("p").build();
        when(usuarioRepository.findByEmail("valid@test.com")).thenReturn(Optional.of(found));
        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createFromDto(dto, creador);

        assertThat(result.getAlumnos()).containsExactly(found);
    }

    @Test
    void createFromDtoShouldHandleOpcionWithNullCorrectaAsDefault() {
        Usuario creador =
                Usuario.builder().id(1L).nombre("P").email("p@t.com").password("p").build();

        CreateCuestionarioRequest.OpcionRequest opReq =
                CreateCuestionarioRequest.OpcionRequest.builder()
                        .texto("Opción")
                        .orden(1)
                        .correcta(null)
                        .build();
        CreateCuestionarioRequest.PreguntaRequest pregReq =
                CreateCuestionarioRequest.PreguntaRequest.builder()
                        .enunciado("Pregunta")
                        .tipo(TipoPregunta.TEST)
                        .opciones(List.of(opReq))
                        .build();
        CreateCuestionarioRequest dto =
                CreateCuestionarioRequest.builder()
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(List.of(pregReq))
                        .build();

        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createFromDto(dto, creador);

        assertThat(result.getPreguntas().get(0).getOpciones().get(0).getCorrecta()).isFalse();
    }

    // ================================================================
    // simple query methods
    // ================================================================

    @Test
    void findByCreadorIdShouldDelegateToRepository() {
        when(cuestionarioRepository.findByCreadorIdOrderByCreatedAtDesc(1L))
                .thenReturn(
                        List.of(
                                Cuestionario.builder()
                                        .id(5L)
                                        .titulo("Q")
                                        .materia("M")
                                        .dificultad(NivelDificultad.BASICO)
                                        .build()));

        List<Cuestionario> result = cuestionarioService.findByCreadorId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5L);
    }

    @Test
    void findByComunidadIdShouldDelegateToRepository() {
        when(cuestionarioRepository.findDistinctByComunidadesIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());

        assertThat(cuestionarioService.findByComunidadId(10L)).isEmpty();
    }

    @Test
    void findAllPublicShouldDelegateToRepository() {
        when(cuestionarioRepository.findPublicGeneralQuizzes()).thenReturn(List.of());

        assertThat(cuestionarioService.findAllPublic()).isEmpty();
    }

    @Test
    void findAssignedToUserShouldDelegateToRepository() {
        when(cuestionarioRepository.findPublishedByAlumnoId(1L)).thenReturn(List.of());

        assertThat(cuestionarioService.findAssignedToUser(1L)).isEmpty();
    }

    @Test
    void findByIdShouldReturnOptional() {
        Cuestionario q =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .build();
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(q));

        assertThat(cuestionarioService.findById(1L)).contains(q);
    }

    @Test
    void findAttemptsByUsuarioAndCuestionarioShouldDelegate() {
        when(intentoRepository.findByUsuarioIdAndCuestionarioIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(List.of());

        assertThat(cuestionarioService.findAttemptsByUsuarioAndCuestionario(1L, 2L)).isEmpty();
    }

    @Test
    void getCuestionarioRepositoryShouldReturnRepository() {
        assertThat(cuestionarioService.getCuestionarioRepository())
                .isEqualTo(cuestionarioRepository);
    }

    // ================================================================
    // updatePublicado
    // ================================================================

    @Test
    void updatePublicadoShouldSetPublicadoAndSave() {
        Cuestionario q =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .publicado(false)
                        .build();
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(q));
        when(cuestionarioRepository.save(any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.updatePublicado(1L, true);

        assertThat(result.getPublicado()).isTrue();
    }

    @Test
    void updatePublicadoShouldThrowWhenNotFound() {
        when(cuestionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuestionarioService.updatePublicado(99L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuestionario no encontrado");
    }

    // ================================================================
    // submitAttempt — TEST / VERDADERO_FALSO questions
    // ================================================================

    @Test
    void submitAttemptShouldEvaluateTestQuestionsCorrectly() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Alumno").email("a@t.com").password("p").build();

        Opcion correcta = Opcion.builder().id(100L).texto("4").correcta(true).build();
        Opcion incorrecta = Opcion.builder().id(101L).texto("5").correcta(false).build();
        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("¿2+2?")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(correcta, incorrecta)))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setOpcionIds(List.of(100L));
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.totalPreguntas()).isEqualTo(1);
        assertThat(result.preguntasCorrectas()).isEqualTo(1);
        assertThat(result.preguntas().get(0).acertada()).isTrue();
        verify(cuestionarioRepository).incrementarIntentos(1L);
    }

    @Test
    void submitAttemptShouldMarkWrongWhenIncorrectOptionSelected() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Opcion correcta = Opcion.builder().id(100L).texto("4").correcta(true).build();
        Opcion incorrecta = Opcion.builder().id(101L).texto("5").correcta(false).build();
        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("¿2+2?")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(correcta, incorrecta)))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setOpcionIds(List.of(101L));
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(0);
        assertThat(result.preguntas().get(0).acertada()).isFalse();
    }

    @Test
    void submitAttemptShouldHandleNoAnswerForTestQuestion() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Opcion correcta = Opcion.builder().id(100L).texto("4").correcta(true).build();
        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("¿2+2?")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(correcta)))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setAnswers(List.of());

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(0);
        assertThat(result.preguntas().get(0).respuestaCorrecta()).containsExactly("4");
    }

    // ================================================================
    // submitAttempt — RESPUESTA_CORTA questions
    // ================================================================

    @Test
    void submitAttemptShouldEvaluateRespuestaCortaCorrectly() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("Capital de España")
                        .tipo(TipoPregunta.RESPUESTA_CORTA)
                        .opciones(new ArrayList<>())
                        .respuestasAceptables(new ArrayList<>(List.of("Madrid", "madrid")))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("Geografía")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setRespuestaTexto(" Madrid ");
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(1);
        assertThat(result.preguntas().get(0).acertada()).isTrue();
    }

    @Test
    void submitAttemptShouldMarkWrongForIncorrectRespuestaCorta() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("Capital de España")
                        .tipo(TipoPregunta.RESPUESTA_CORTA)
                        .opciones(new ArrayList<>())
                        .respuestasAceptables(new ArrayList<>(List.of("Madrid")))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("Geografía")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setRespuestaTexto("Barcelona");
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(0);
        assertThat(result.preguntas().get(0).acertada()).isFalse();
    }

    @Test
    void submitAttemptShouldHandleNullRespuestaTexto() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("Capital")
                        .tipo(TipoPregunta.RESPUESTA_CORTA)
                        .opciones(new ArrayList<>())
                        .respuestasAceptables(new ArrayList<>(List.of("Madrid")))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("G")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setRespuestaTexto(null);
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(0);
    }

    // ================================================================
    // submitAttempt — VERDADERO_FALSO
    // ================================================================

    @Test
    void submitAttemptShouldEvaluateVerdaderoFalsoCorrectly() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Opcion opTrue = Opcion.builder().id(1L).texto("Verdadero").correcta(true).build();
        Opcion opFalse = Opcion.builder().id(2L).texto("Falso").correcta(false).build();
        Pregunta pregunta =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("¿El agua hierve a 100°C?")
                        .tipo(TipoPregunta.VERDADERO_FALSO)
                        .opciones(new ArrayList<>(List.of(opTrue, opFalse)))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Ciencias")
                        .materia("Física")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(pregunta)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer answer = new SubmitAttemptRequest.Answer();
        answer.setPreguntaId(10L);
        answer.setOpcionIds(List.of(1L));
        request.setAnswers(List.of(answer));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(1);
    }

    // ================================================================
    // submitAttempt — mixed questions score calculation
    // ================================================================

    @Test
    void submitAttemptShouldCalculateScoreAcrossMultipleQuestions() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Opcion op1 = Opcion.builder().id(10L).texto("Sí").correcta(true).build();
        Pregunta p1 =
                Pregunta.builder()
                        .id(1L)
                        .enunciado("P1")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(op1)))
                        .build();

        Pregunta p2 =
                Pregunta.builder()
                        .id(2L)
                        .enunciado("P2")
                        .tipo(TipoPregunta.RESPUESTA_CORTA)
                        .opciones(new ArrayList<>())
                        .respuestasAceptables(new ArrayList<>(List.of("42")))
                        .build();

        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Mix")
                        .materia("M")
                        .dificultad(NivelDificultad.INTERMEDIO)
                        .preguntas(new ArrayList<>(List.of(p1, p2)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        SubmitAttemptRequest.Answer a1 = new SubmitAttemptRequest.Answer();
        a1.setPreguntaId(1L);
        a1.setOpcionIds(List.of(10L));
        SubmitAttemptRequest.Answer a2 = new SubmitAttemptRequest.Answer();
        a2.setPreguntaId(2L);
        a2.setRespuestaTexto("wrong");
        request.setAnswers(List.of(a1, a2));

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.totalPreguntas()).isEqualTo(2);
        assertThat(result.preguntasCorrectas()).isEqualTo(1);
        assertThat(result.intento().getPuntuacion()).isEqualTo(50.0);
    }

    @Test
    void submitAttemptShouldThrowWhenCuestionarioNotFound() {
        when(cuestionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                cuestionarioService.submitAttempt(
                                        99L,
                                        new SubmitAttemptRequest(),
                                        Usuario.builder()
                                                .id(1L)
                                                .nombre("X")
                                                .email("x@x.com")
                                                .password("p")
                                                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuestionario no encontrado");
    }

    @Test
    void submitAttemptShouldHandleNullAnswersList() {
        Usuario usuario =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();

        Opcion op = Opcion.builder().id(1L).texto("A").correcta(true).build();
        Pregunta p =
                Pregunta.builder()
                        .id(10L)
                        .enunciado("P")
                        .tipo(TipoPregunta.TEST)
                        .opciones(new ArrayList<>(List.of(op)))
                        .build();
        Cuestionario cuestionario =
                Cuestionario.builder()
                        .id(1L)
                        .titulo("Q")
                        .materia("M")
                        .dificultad(NivelDificultad.BASICO)
                        .preguntas(new ArrayList<>(List.of(p)))
                        .build();

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setAnswers(null);

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(intentoRepository.save(any(CuestionarioIntento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioService.AttemptSubmissionResult result =
                cuestionarioService.submitAttempt(1L, request, usuario);

        assertThat(result.preguntasCorrectas()).isEqualTo(0);
    }
}
