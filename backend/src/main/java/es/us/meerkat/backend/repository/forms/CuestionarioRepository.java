package es.us.meerkat.backend.repository.forms;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.NivelDificultad;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long> {

    List<Cuestionario> findByCreadorIdOrderByCreatedAtDesc(Long creadorId);

    List<Cuestionario> findDistinctByComunidadesIdOrderByCreatedAtDesc(Long comunidadId);

    /**
     * Busca cuestionarios activos por materia o tags. Usado para generar recomendaciones basadas en
     * temas de interés.
     */
    @Query(
            """
                SELECT DISTINCT q FROM Cuestionario q
                LEFT JOIN q.tags t
                WHERE q.activo = true
                  AND (
                    LOWER(q.materia) IN :temas
                    OR LOWER(t)       IN :temas
                  )
                ORDER BY q.intentos DESC
            """)
    List<Cuestionario> findActivosByTemasInteres(@Param("temas") List<String> temas);

    /**
     * Busca cuestionarios activos filtrando por materia/tags Y dificultad. Usado para adaptar la
     * dificultad al rendimiento del usuario.
     */
    @Query(
            """
                SELECT DISTINCT q FROM Cuestionario q
                LEFT JOIN q.tags t
                WHERE q.activo = true
                  AND q.dificultad = :dificultad
                  AND (
                    LOWER(q.materia) IN :temas
                    OR LOWER(t)       IN :temas
                  )
                ORDER BY q.intentos DESC
            """)
    List<Cuestionario> findActivosByTemasYDificultad(
            @Param("temas") List<String> temas, @Param("dificultad") NivelDificultad dificultad);

    /**
     * Cuestionarios ya intentados por el usuario — para no repetir los recientes. Se obtienen a
     * partir de ActividadUsuario con tipoActividad = 'QUIZ_COMPLETADO'.
     */
    @Query(
            """
                SELECT DISTINCT q FROM Cuestionario q
                WHERE q.id IN :ids
            """)
    List<Cuestionario> findByIdIn(@Param("ids") List<Long> ids);

    /** Lista todos los cuestionarios públicos generales (sin comunidades ni alumnos asignados). */
    @Query(
            """
                SELECT q FROM Cuestionario q
                WHERE q.publicado = true AND q.activo = true
                  AND q.comunidades IS EMPTY AND q.alumnos IS EMPTY
                ORDER BY q.createdAt DESC
            """)
    List<Cuestionario> findPublicGeneralQuizzes();

    /** Lista cuestionarios publicados asignados a un alumno concreto. */
    @Query(
            """
                SELECT q FROM Cuestionario q
                JOIN q.alumnos a
                WHERE a.id = :userId AND q.publicado = true AND q.activo = true
                ORDER BY q.createdAt DESC
            """)
    List<Cuestionario> findPublishedByAlumnoId(@Param("userId") Long userId);

    /** Incrementa el contador de intentos. */
    @Modifying
    @Query("UPDATE Cuestionario q SET q.intentos = q.intentos + 1 WHERE q.id = :id")
    void incrementarIntentos(@Param("id") Long id);
}
