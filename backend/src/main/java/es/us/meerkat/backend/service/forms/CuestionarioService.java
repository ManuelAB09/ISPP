package es.us.meerkat.backend.service.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.forms.CreateCuestionarioRequest;
import es.us.meerkat.backend.dto.forms.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.CuestionarioIntento;
import es.us.meerkat.backend.entity.forms.Opcion;
import es.us.meerkat.backend.entity.forms.Pregunta;
import es.us.meerkat.backend.entity.recommendations.TipoPregunta;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioIntentoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuestionarioService {

    private final CuestionarioRepository cuestionarioRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuestionarioIntentoRepository intentoRepository;

    /**
     * Crea un cuestionario con preguntas y opciones anidadas. Se asegura de enlazar las relaciones
     * bidireccionales antes de persistir.
     */
    @Transactional
    public Cuestionario createCuestionario(Cuestionario cuestionario) {
        if (cuestionario.getPreguntas() != null) {
            for (Pregunta p : cuestionario.getPreguntas()) {
                p.setCuestionario(cuestionario);
                if (p.getOpciones() != null) {
                    for (Opcion o : p.getOpciones()) {
                        o.setPregunta(p);
                    }
                }
            }
            cuestionario.setNumPreguntas(cuestionario.getPreguntas().size());
        }

        return cuestionarioRepository.save(cuestionario);
    }

    /**
     * Crea un cuestionario a partir del DTO `CreateCuestionarioRequest`. Resuelve comunidades y
     * alumnos por sus ids.
     */
    @Transactional
    public Cuestionario createFromDto(CreateCuestionarioRequest dto, Usuario creador) {
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setTitulo(dto.getTitulo());
        cuestionario.setDescripcion(dto.getDescripcion());
        cuestionario.setImagenUrl(dto.getImagenUrl());
        cuestionario.setMateria(dto.getMateria());
        cuestionario.setTags(dto.getTags() != null ? dto.getTags() : new java.util.ArrayList<>());
        cuestionario.setDificultad(dto.getDificultad());
        cuestionario.setNivelEducativo(dto.getNivelEducativo());
        cuestionario.setNumPreguntas(dto.getNumPreguntas() != null ? dto.getNumPreguntas() : 0);
        cuestionario.setTiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos());
        cuestionario.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        cuestionario.setPublicado(dto.getPublicado() != null ? dto.getPublicado() : false);
        cuestionario.setCreador(creador);
        cuestionario.setCreatedAt(java.time.LocalDateTime.now());
        cuestionario.setIntentos(0L);
        cuestionario.setPreguntas(new java.util.ArrayList<>());
        cuestionario.setComunidades(new java.util.HashSet<>());
        cuestionario.setAlumnos(new java.util.HashSet<>());

        // preguntas
        if (dto.getPreguntas() != null) {
            for (CreateCuestionarioRequest.PreguntaRequest pr : dto.getPreguntas()) {
                Pregunta p = new Pregunta();
                p.setEnunciado(pr.getEnunciado());
                p.setTipo(pr.getTipo());
                p.setOpciones(new java.util.ArrayList<>());
                p.setRespuestasAceptables(
                        pr.getRespuestasAceptables() != null
                                ? pr.getRespuestasAceptables()
                                : new java.util.ArrayList<>());

                if (pr.getOpciones() != null) {
                    for (CreateCuestionarioRequest.OpcionRequest or : pr.getOpciones()) {
                        Opcion o = new Opcion();
                        o.setTexto(or.getTexto());
                        o.setOrden(or.getOrden());
                        o.setCorrecta(or.getCorrecta() != null ? or.getCorrecta() : false);
                        o.setPregunta(p);
                        p.getOpciones().add(o);
                    }
                }

                p.setCuestionario(cuestionario);
                cuestionario.getPreguntas().add(p);
            }
            cuestionario.setNumPreguntas(cuestionario.getPreguntas().size());
        }

        // comunidades
        if (dto.getComunidadesIds() != null && !dto.getComunidadesIds().isEmpty()) {
            java.util.List<Comunidad> found =
                    comunidadRepository.findAllById(dto.getComunidadesIds());
            cuestionario.getComunidades().addAll(found);
        }

        // alumnos por ids
        if (dto.getAlumnosIds() != null && !dto.getAlumnosIds().isEmpty()) {
            java.util.List<Usuario> users = usuarioRepository.findAllById(dto.getAlumnosIds());
            cuestionario.getAlumnos().addAll(users);
        }

        // alumnos por emails
        if (dto.getAlumnosEmails() != null && !dto.getAlumnosEmails().isEmpty()) {
            for (String email : dto.getAlumnosEmails()) {
                if (email != null && !email.isBlank()) {
                    usuarioRepository
                            .findByEmail(email.trim())
                            .ifPresent(u -> cuestionario.getAlumnos().add(u));
                }
            }
        }

        return cuestionarioRepository.save(cuestionario);
    }

    public java.util.List<Cuestionario> findByCreadorId(Long creadorId) {
        return cuestionarioRepository.findByCreadorIdOrderByCreatedAtDesc(creadorId);
    }

    public java.util.List<Cuestionario> findByComunidadId(Long comunidadId) {
        return cuestionarioRepository.findDistinctByComunidadesIdOrderByCreatedAtDesc(comunidadId);
    }

    public List<CuestionarioIntento> findAttemptsByUsuarioAndCuestionario(
            Long usuarioId, Long cuestionarioId) {
        return intentoRepository.findByUsuarioIdAndCuestionarioIdOrderByCreatedAtDesc(
                usuarioId, cuestionarioId);
    }

    /** Registra un intento enviado por un alumno y devuelve resultado detallado. */
    @Transactional
    public AttemptSubmissionResult submitAttempt(
            Long cuestionarioId, SubmitAttemptRequest request, Usuario usuario) {

        Cuestionario c =
                cuestionarioRepository
                        .findById(cuestionarioId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cuestionario no encontrado"));

        // UC-65: si el cuestionario tiene tiempo límite, rechazar el envío cuando se ha superado.
        Integer limiteMinutos = c.getTiempoEstimadoMinutos();
        if (limiteMinutos != null
                && limiteMinutos > 0
                && request.getTiempoEmpleadoSegundos() != null) {
            long limiteSegundos = limiteMinutos.longValue() * 60L;
            long graciaSegundos = 15L;
            if (request.getTiempoEmpleadoSegundos() > limiteSegundos + graciaSegundos) {
                throw new ValidationException(
                        "Se ha superado el tiempo límite del cuestionario; el envío no es válido.");
            }
        }

        // Map preguntaId -> answer for quick lookup
        Map<Long, SubmitAttemptRequest.Answer> answersMap = new HashMap<>();
        if (request.getAnswers() != null) {
            for (SubmitAttemptRequest.Answer a : request.getAnswers()) {
                answersMap.put(a.getPreguntaId(), a);
            }
        }

        int total = c.getPreguntas() != null ? c.getPreguntas().size() : 0;
        int correct = 0;
        List<QuestionEvaluation> questionsEvaluation = new ArrayList<>();

        for (Pregunta p : c.getPreguntas()) {
            SubmitAttemptRequest.Answer a = answersMap.get(p.getId());
            boolean acertada = false;
            List<String> respuestaUsuario = new ArrayList<>();
            List<String> respuestaCorrecta = new ArrayList<>();

            if (p.getTipo() == TipoPregunta.TEST || p.getTipo() == TipoPregunta.VERDADERO_FALSO) {
                if (a != null && a.getOpcionIds() != null && !a.getOpcionIds().isEmpty()) {
                    Set<Long> selected = new HashSet<>(a.getOpcionIds());
                    Set<Long> correctIds = new HashSet<>();
                    for (Opcion o : p.getOpciones()) {
                        if (Boolean.TRUE.equals(o.getCorrecta())) {
                            correctIds.add(o.getId());
                            respuestaCorrecta.add(o.getTexto());
                        }
                        if (selected.contains(o.getId())) {
                            respuestaUsuario.add(o.getTexto());
                        }
                    }
                    // consider correct if sets equal
                    acertada = selected.equals(correctIds);
                } else {
                    for (Opcion o : p.getOpciones()) {
                        if (Boolean.TRUE.equals(o.getCorrecta())) {
                            respuestaCorrecta.add(o.getTexto());
                        }
                    }
                }
            } else if (p.getTipo() == TipoPregunta.RESPUESTA_CORTA) {
                for (String ok : p.getRespuestasAceptables()) {
                    if (ok != null && !ok.isBlank()) {
                        respuestaCorrecta.add(ok.trim());
                    }
                }

                if (a != null && a.getRespuestaTexto() != null) {
                    String submitted = a.getRespuestaTexto().trim().toLowerCase();
                    if (!submitted.isBlank()) {
                        respuestaUsuario.add(a.getRespuestaTexto().trim());
                    }
                    for (String ok : p.getRespuestasAceptables()) {
                        if (ok != null && submitted.equals(ok.trim().toLowerCase())) {
                            acertada = true;
                            break;
                        }
                    }
                }
            }

            if (acertada) {
                correct++;
            }

            questionsEvaluation.add(
                    new QuestionEvaluation(
                            p.getId(),
                            p.getEnunciado(),
                            p.getTipo(),
                            acertada,
                            respuestaUsuario,
                            respuestaCorrecta));
        }

        double score = 0d;
        if (total > 0) {
            score = (correct * 100.0d) / total;
        }

        CuestionarioIntento intento = new CuestionarioIntento();
        intento.setCuestionario(c);
        intento.setUsuario(usuario);
        intento.setPuntuacion(score);
        intento.setCreatedAt(java.time.LocalDateTime.now());

        // persist intento
        CuestionarioIntento saved = intentoRepository.save(intento);

        // incrementar contador de intentos en cuestionario
        cuestionarioRepository.incrementarIntentos(c.getId());

        return new AttemptSubmissionResult(saved, total, correct, questionsEvaluation);
    }

    public java.util.List<Cuestionario> findAllPublic() {
        return cuestionarioRepository.findPublicGeneralQuizzes();
    }

    public java.util.List<Cuestionario> findAssignedToUser(Long userId) {
        return cuestionarioRepository.findPublishedByAlumnoId(userId);
    }

    public java.util.Optional<Cuestionario> findById(Long id) {
        return cuestionarioRepository.findById(id);
    }

    // Expose repository for simple use by controller in this change set
    public CuestionarioRepository getCuestionarioRepository() {
        return cuestionarioRepository;
    }

    @Transactional
    public Cuestionario updatePublicado(Long id, Boolean publicado) {
        Cuestionario c =
                cuestionarioRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cuestionario no encontrado"));
        c.setPublicado(publicado);
        return cuestionarioRepository.save(c);
    }

    public record QuestionEvaluation(
            Long preguntaId,
            String enunciado,
            TipoPregunta tipo,
            boolean acertada,
            List<String> respuestaUsuario,
            List<String> respuestaCorrecta) {}

    public record AttemptSubmissionResult(
            CuestionarioIntento intento,
            int totalPreguntas,
            int preguntasCorrectas,
            List<QuestionEvaluation> preguntas) {}
}
