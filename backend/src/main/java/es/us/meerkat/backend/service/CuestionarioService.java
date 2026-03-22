package es.us.meerkat.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.CreateCuestionarioRequest;
import es.us.meerkat.backend.dto.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Cuestionario;
import es.us.meerkat.backend.entity.CuestionarioIntento;
import es.us.meerkat.backend.entity.Opcion;
import es.us.meerkat.backend.entity.Pregunta;
import es.us.meerkat.backend.entity.TipoPregunta;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.CuestionarioIntentoRepository;
import es.us.meerkat.backend.repository.CuestionarioRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
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
        cuestionario.setTags(dto.getTags());
        cuestionario.setDificultad(dto.getDificultad());
        cuestionario.setNivelEducativo(dto.getNivelEducativo());
        cuestionario.setNumPreguntas(dto.getNumPreguntas() != null ? dto.getNumPreguntas() : 0);
        cuestionario.setTiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos());
        cuestionario.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        cuestionario.setPublicado(dto.getPublicado() != null ? dto.getPublicado() : false);
        cuestionario.setCreador(creador);

        // preguntas
        if (dto.getPreguntas() != null) {
            for (CreateCuestionarioRequest.PreguntaRequest pr : dto.getPreguntas()) {
                Pregunta p = new Pregunta();
                p.setEnunciado(pr.getEnunciado());
                p.setTipo(pr.getTipo());
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

        // alumnos
        if (dto.getAlumnosIds() != null && !dto.getAlumnosIds().isEmpty()) {
            java.util.List<Usuario> users = usuarioRepository.findAllById(dto.getAlumnosIds());
            cuestionario.getAlumnos().addAll(users);
        }

        return cuestionarioRepository.save(cuestionario);
    }

    public java.util.List<Cuestionario> findByCreadorId(Long creadorId) {
        return cuestionarioRepository.findByCreadorIdOrderByCreatedAtDesc(creadorId);
    }

    public java.util.List<Cuestionario> findByComunidadId(Long comunidadId) {
        return cuestionarioRepository.findDistinctByComunidadesIdOrderByCreatedAtDesc(comunidadId);
    }

    /** Registra un intento enviado por un alumno y calcula la puntuación. */
    @Transactional
    public CuestionarioIntento submitAttempt(
            Long cuestionarioId, SubmitAttemptRequest request, Usuario usuario) {

        Cuestionario c =
                cuestionarioRepository
                        .findById(cuestionarioId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cuestionario no encontrado"));

        // Map preguntaId -> answer for quick lookup
        java.util.Map<Long, SubmitAttemptRequest.Answer> answersMap = new java.util.HashMap<>();
        if (request.getAnswers() != null) {
            for (SubmitAttemptRequest.Answer a : request.getAnswers()) {
                answersMap.put(a.getPreguntaId(), a);
            }
        }

        int total = c.getPreguntas() != null ? c.getPreguntas().size() : 0;
        int correct = 0;

        for (Pregunta p : c.getPreguntas()) {
            SubmitAttemptRequest.Answer a = answersMap.get(p.getId());
            boolean acertada = false;
            if (p.getTipo() == TipoPregunta.TEST || p.getTipo() == TipoPregunta.VERDADERO_FALSO) {
                if (a != null && a.getOpcionIds() != null && !a.getOpcionIds().isEmpty()) {
                    java.util.Set<Long> selected = new java.util.HashSet<>(a.getOpcionIds());
                    java.util.Set<Long> correctIds = new java.util.HashSet<>();
                    for (Opcion o : p.getOpciones()) {
                        if (Boolean.TRUE.equals(o.getCorrecta())) {
                            correctIds.add(o.getId());
                        }
                    }
                    // consider correct if sets equal
                    acertada = selected.equals(correctIds);
                }
            } else if (p.getTipo() == TipoPregunta.RESPUESTA_CORTA) {
                if (a != null && a.getRespuestaTexto() != null) {
                    String submitted = a.getRespuestaTexto().trim().toLowerCase();
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

        return saved;
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
}
