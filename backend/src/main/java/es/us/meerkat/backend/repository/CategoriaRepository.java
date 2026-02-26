package es.us.meerkat.backend.repository;

import es.us.meerkat.backend.entity.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByComunidadIdOrderByOrden(Long comunidadId);
}
