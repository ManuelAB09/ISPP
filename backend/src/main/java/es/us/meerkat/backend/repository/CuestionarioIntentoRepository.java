package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.CuestionarioIntento;

@Repository
public interface CuestionarioIntentoRepository extends JpaRepository<CuestionarioIntento, Long> {
    List<CuestionarioIntento> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    @Query(
            "SELECT ci FROM CuestionarioIntento ci JOIN FETCH ci.cuestionario WHERE ci.usuario.id ="
                    + " :usuarioId ORDER BY ci.createdAt DESC")
    List<CuestionarioIntento> findWithCuestionarioByUsuarioId(
            @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
