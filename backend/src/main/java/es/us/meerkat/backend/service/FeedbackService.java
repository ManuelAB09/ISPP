package es.us.meerkat.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Feedback;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.FeedbackRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadRepository comunidadRepository;
    private final AuthorizationService authorizationService;

    public Feedback createFeedback(
            Long profesorId,
            Long alumnoId,
            Long comunidadId,
            String contenido,
            Integer calificacion) {
        Usuario profesor =
                usuarioRepository
                        .findById(profesorId)
                        .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        Usuario alumno =
                usuarioRepository
                        .findById(alumnoId)
                        .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        RolComunidad rol = authorizationService.getUserRoleInCommunity(profesorId, comunidadId);
        if (rol == null || (rol != RolComunidad.PROFESOR && rol != RolComunidad.ADMIN)) {
            throw new IllegalArgumentException(
                    "Solo profesores o administradores pueden dar feedback");
        }

        if (!authorizationService.isMemberOf(alumnoId, comunidadId)) {
            throw new IllegalArgumentException("El alumno no es miembro de la comunidad");
        }

        Feedback fb =
                Feedback.builder()
                        .profesor(profesor)
                        .alumno(alumno)
                        .comunidad(comunidad)
                        .contenido(contenido)
                        .calificacion(calificacion)
                        .build();

        return feedbackRepository.save(fb);
    }

    @Transactional(readOnly = true)
    public Page<Feedback> listFeedbacksByCommunity(Long comunidadId, Pageable pageable) {
        return feedbackRepository.findByComunidadId(comunidadId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Feedback> listFeedbacksForStudent(Long alumnoId, Pageable pageable) {
        return feedbackRepository.findByAlumnoId(alumnoId, pageable);
    }
}
