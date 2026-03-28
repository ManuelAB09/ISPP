package es.us.meerkat.backend.repository.recommendations;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.us.meerkat.backend.entity.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {
    List<Valoracion> findByProfesorId(Long profesorId);

    boolean existsByAlumnoIdAndEventoId(Long alumnoId, Long eventoId);

    @Query("SELECT AVG(v.puntuacion) FROM Valoracion v WHERE v.profesor.id = :profesorId")
    Double findMediaByProfesorId(Long profesorId);

    @Query("SELECT COUNT(v) FROM Valoracion v WHERE v.profesor.id = :profesorId")
    Long countByProfesorId(Long profesorId);
}
