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
        Valoracion saved = valoracionRepository.save(valoracion);
        actualizarNivelDesempeno(valoracion.getProfesor().getId());
        return saved;
    }

    private void actualizarNivelDesempeno(Long profesorId) {
        Tutor tutor = tutorRepository.findById(profesorId).orElse(null);
        if (tutor == null) {
            return;
        }
        Long total = valoracionRepository.countByProfesorId(profesorId);
        Double media = valoracionRepository.findMediaByProfesorId(profesorId);
        String nivel = "principiante";
        if (total != null && media != null) {
            if (total >= 10 && total <= 50 && media >= 3) {
                nivel = "avanzado";
            } else if (total > 50 && media >= 4.5) {
                nivel = "experto";
            } else if (total < 10 || media < 3) {
                nivel = "principiante";
            }
        }
        tutor.setNivelDesempeno(nivel);
        tutorRepository.save(tutor);
    }

    public List<Valoracion> obtenerValoracionesPorProfesor(Long profesorId) {
        return valoracionRepository.findByProfesor_Id(profesorId);
    }

    public Double obtenerMediaPorProfesor(Long profesorId) {
        return valoracionRepository.findMediaByProfesorId(profesorId);
    }

    public Long contarPorProfesor(Long profesorId) {
        return valoracionRepository.countByProfesorId(profesorId);
    }
}
