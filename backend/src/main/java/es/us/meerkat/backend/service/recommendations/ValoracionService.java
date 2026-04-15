package es.us.meerkat.backend.service.recommendations;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.us.meerkat.backend.entity.recommendations.Valoracion;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.repository.recommendations.ValoracionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;

@Service
public class ValoracionService {
    @Autowired private ValoracionRepository valoracionRepository;
    @Autowired private TutorRepository tutorRepository;

    public Valoracion guardarValoracion(Valoracion valoracion) {
        // Validar que el alumno no sea el propio tutor
        if (valoracion.getProfesor() != null && valoracion.getAlumno() != null) {
            Tutor tutor =
                    tutorRepository
                            .findById(valoracion.getProfesor().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
            if (tutor.getUsuario().getId().equals(valoracion.getAlumno().getId())) {
                throw new IllegalArgumentException("No puedes valorarte a ti mismo");
            }
        }
        // Validar que el alumno no haya valorado ya este evento
        if (valoracion.getAlumno() != null && valoracion.getEvento() != null) {
            if (valoracionRepository.existsByAlumnoIdAndEventoId(
                    valoracion.getAlumno().getId(), valoracion.getEvento().getId())) {
                throw new IllegalArgumentException("Ya has valorado este evento");
            }
        }
        return valoracionRepository.save(valoracion);
    }

    public boolean isAlreadyRated(Long alumnoId, Long eventoId) {
        return valoracionRepository.existsByAlumnoIdAndEventoId(alumnoId, eventoId);
    }

    public List<Valoracion> obtenerValoracionesPorProfesor(Long profesorId) {
        return valoracionRepository.findByProfesorId(profesorId);
    }

    public Double obtenerMediaPorProfesor(Long profesorId) {
        return valoracionRepository.findMediaByProfesorId(profesorId);
    }

    public Long contarValoracionesPorProfesor(Long profesorId) {
        return valoracionRepository.countByProfesorId(profesorId);
    }

    public String calcularNivel(Long profesorId) {
        Long total = contarValoracionesPorProfesor(profesorId);
        Double media = obtenerMediaPorProfesor(profesorId);
        if (total == null || media == null) {
            return "principiante";
        }
        if (total < 10 || media < 3) {
            return "principiante";
        }
        if (total <= 50 && media >= 3) {
            return "avanzado";
        }
        if (total > 50 && media >= 4.5) {
            return "experto";
        }
        return "principiante";
    }
}
