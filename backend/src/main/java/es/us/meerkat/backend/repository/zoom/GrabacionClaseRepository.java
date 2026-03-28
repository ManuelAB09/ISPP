package es.us.meerkat.backend.repository.zoom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.GrabacionClase;

/** Repositorio JPA para la entidad {@link GrabacionClase}. */
public interface GrabacionClaseRepository extends JpaRepository<GrabacionClase, Long> {

    /** Obtiene todas las grabaciones de una comunidad. */
    List<GrabacionClase> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    /** Obtiene grabaciones sincronizadas con Classroom. */
    List<GrabacionClase> findByComunidadIdAndSincronizadoClassroomTrue(Long comunidadId);

    /** Busca una grabación por su ID en Google Drive. */
    Optional<GrabacionClase> findByGoogleDriveFileId(String googleDriveFileId);

    /** Obtiene grabaciones de un evento específico. */
    List<GrabacionClase> findByEventoIdOrderByCreatedAtDesc(Long eventoId);

    /** Cuenta grabaciones en una comunidad. */
    long countByComunidadId(Long comunidadId);

    /** Busca grabaciones creadas en un período. */
    @Query(
            """
            SELECT g FROM GrabacionClase g
            WHERE g.comunidad.id = :comunidadId
            AND g.createdAt BETWEEN :desde AND :hasta
            ORDER BY g.createdAt DESC
            """)
    List<GrabacionClase> findGrabacionesPorPeriodo(
            @Param("comunidadId") Long comunidadId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
