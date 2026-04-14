package es.us.meerkat.backend.repository.recommendations;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.google.Contenido;

@Repository
public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    /**
     * Busca contenidos activos cuya materia o tags coincidan con alguno de los temas de interés del
     * usuario. Usado por el motor de recomendaciones.
     */
    @Query(
            """
                SELECT DISTINCT c FROM Contenido c
                LEFT JOIN c.tags t
                WHERE c.activo = true
                  AND (
                    LOWER(c.materia) IN :temas
                    OR LOWER(t)       IN :temas
                  )
                ORDER BY c.visualizaciones DESC
            """)
    List<Contenido> findActivosByTemasInteres(@Param("temas") List<String> temas);

    /** Busca contenidos activos filtrando además por nivel educativo del usuario. */
    @Query(
            """
                SELECT DISTINCT c FROM Contenido c
                LEFT JOIN c.tags t
                WHERE c.activo = true
                  AND (c.nivelEducativo IS NULL OR LOWER(c.nivelEducativo) = LOWER(:nivel))
                  AND (
                    LOWER(c.materia) IN :temas
                    OR LOWER(t)       IN :temas
                  )
                ORDER BY c.visualizaciones DESC
            """)
    List<Contenido> findActivosByTemasYNivel(
            @Param("temas") List<String> temas, @Param("nivel") String nivel);

    /** Incrementa el contador de visualizaciones. */
    @Modifying
    @Query("UPDATE Contenido c SET c.visualizaciones = c.visualizaciones + 1 WHERE c.id = :id")
    void incrementarVisualizaciones(@Param("id") Long id);

    /** Contenidos más vistos (para trending). */
    @Query(
            "SELECT c FROM Contenido c WHERE c.activo = true ORDER BY c.visualizaciones DESC LIMIT"
                    + " :limite")
    List<Contenido> findTrending(@Param("limite") int limite);
}
