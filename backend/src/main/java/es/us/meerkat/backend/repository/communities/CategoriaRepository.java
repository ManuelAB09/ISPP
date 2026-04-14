package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByComunidadIdOrderByOrden(Long comunidadId);
}
