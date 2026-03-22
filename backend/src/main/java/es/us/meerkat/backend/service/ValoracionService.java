package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Valoracion;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.ValoracionRepository;

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
        return valoracionRepository.save(valoracion);
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
