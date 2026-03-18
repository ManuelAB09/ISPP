package es.us.meerkat.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.CreateCuestionarioRequest;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Cuestionario;
import es.us.meerkat.backend.entity.Opcion;
import es.us.meerkat.backend.entity.Pregunta;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.CuestionarioRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuestionarioService {

    private final CuestionarioRepository cuestionarioRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;

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
    public Cuestionario createFromDto(CreateCuestionarioRequest dto) {
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
