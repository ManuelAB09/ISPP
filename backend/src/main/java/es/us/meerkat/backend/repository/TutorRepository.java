package es.us.meerkat.backend.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link Tutor}.
 *
 * <p>Permite realizar operaciones CRUD y consultas específicas sobre tutores en la base de datos.
 */
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    /**
     * Busca un tutor asociado a un usuario específico.
     *
     * @param us Usuario asociado al tutor.
     * @return Optional que contiene el tutor si existe.
     */
    Optional<Tutor> findByUs(Usuario us);

    /**
     * Devuelve todos los tutores que estén verificados.
     *
     * @return Lista de tutores verificados.
     */
    List<Tutor> findByVerificadoTrue();

    /**
     * Busca un tutor asociado a un usuario específico.
     *
     * @param tutorId id del tutor en el que esta.
     * @param usuarioId id del user logueado.
     * @return Optional que contiene el tutor si existe.
     */
    Optional<Tutor> findByIdAndUsId(Long tutorId, Long usuarioId);

    /**
     * Busca un todos los tutores asociado a un usuario específico.
     *
     * @param usuarioId id del user logueado.
     * @return Optional que contiene el tutor si existe.
     */
    List<Tutor> findAllByUsId(Long usuarioId);

    /**
     * Devuelve todos los tutores verificados con paginación. Utiliza @EntityGraph para cargar la
     * relación con Usuario de forma eager.
     *
     * @param pageable Información de paginación.
     * @return Página de tutores verificados.
     */
    @EntityGraph(attributePaths = {"us"})
    Page<Tutor> findByVerificadoTrue(Pageable pageable);

    /**
     * Busca tutores verificados filtrando por especialidad (JOIN sobre ElementCollection) y rango
     * de tarifa. Utiliza @EntityGraph para cargar la relación con Usuario de forma eager.
     *
     * @param especialidad Especialidad buscada (contiene, case-insensitive)
     * @param tarifaMin Tarifa mínima
     * @param tarifaMax Tarifa máxima
     * @param pageable Información de paginación
     * @return Página de tutores filtrados
     */
    @EntityGraph(attributePaths = {"us"})
    @Query(
            "SELECT DISTINCT t FROM Tutor t JOIN t.especialidades e "
                    + "WHERE t.verificado = true "
                    + "AND LOWER(e) LIKE LOWER(CONCAT('%', :especialidad, '%')) "
                    + "AND t.tarifaHora BETWEEN :tarifaMin AND :tarifaMax")
    Page<Tutor> findVerificadosByEspecialidadAndTarifa(
            @Param("especialidad") String especialidad,
            @Param("tarifaMin") BigDecimal tarifaMin,
            @Param("tarifaMax") BigDecimal tarifaMax,
            Pageable pageable);

    @EntityGraph(attributePaths = {"us"})
    @Query(
            """
    SELECT DISTINCT t
    FROM Tutor t
    LEFT JOIN t.especialidades e
    WHERE t.verificado = true
    AND (:especialidad IS NULL OR LOWER(e) LIKE LOWER(CONCAT('%', CAST(:especialidad AS string), '%')))
    AND (:tarifaMin IS NULL OR t.tarifaHora >= :tarifaMin)
    AND (:tarifaMax IS NULL OR t.tarifaHora <= :tarifaMax)
""")
    Page<Tutor> findVerificadosFiltrados(
            @Param("especialidad") String especialidad,
            @Param("tarifaMin") BigDecimal tarifaMin,
            @Param("tarifaMax") BigDecimal tarifaMax,
            Pageable pageable);
}
