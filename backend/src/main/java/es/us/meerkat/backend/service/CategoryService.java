package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Categoria;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.repository.CategoriaRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoriaRepository categoriaRepository;
    private final ComunidadRepository comunidadRepository;
    private final AuthorizationService authorizationService;

    /** Crea una nueva categoría (solo ADMIN). */
    public Categoria createCategory(
            Long userId, Long communityId, String nombre, String descripcion) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden crear categorías");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Obtener el próximo orden
        List<Categoria> categorias = categoriaRepository.findByComunidadIdOrderByOrden(communityId);
        int nextOrder =
                categorias.isEmpty() ? 1 : categorias.get(categorias.size() - 1).getOrden() + 1;

        Categoria categoria =
                Categoria.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .orden(nextOrder)
                        .comunidad(comunidad)
                        .build();

        return categoriaRepository.save(categoria);
    }

    /** Actualiza una categoría (solo ADMIN). */
    public Categoria updateCategory(
            Long userId, Long communityId, Long categoryId, String nombre, String descripcion) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden actualizar categorías");
        }

        Categoria categoria =
                categoriaRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        // Verificar que la categoría pertenezca a esta comunidad
        if (!categoria.getComunidad().getId().equals(communityId)) {
            throw new IllegalArgumentException("La categoría no pertenece a esta comunidad");
        }

        if (nombre != null && !nombre.isBlank()) {
            categoria.setNombre(nombre);
        }
        if (descripcion != null) {
            categoria.setDescripcion(descripcion);
        }

        return categoriaRepository.save(categoria);
    }

    /** Elimina una categoría (solo ADMIN). */
    public void deleteCategory(Long userId, Long communityId, Long categoryId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden eliminar categorías");
        }

        Categoria categoria =
                categoriaRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        // Verificar que la categoría pertenezca a esta comunidad
        if (!categoria.getComunidad().getId().equals(communityId)) {
            throw new IllegalArgumentException("La categoría no pertenece a esta comunidad");
        }

        categoriaRepository.delete(categoria);
    }

    /** Lista las categorías de una comunidad (ordenadas). */
    @Transactional(readOnly = true)
    public List<Categoria> listCategories(Long communityId) {
        return categoriaRepository.findByComunidadIdOrderByOrden(communityId);
    }

    /** Reordena las categorías de una comunidad (solo ADMIN). */
    public List<Categoria> reorderCategories(
            Long userId, Long communityId, List<Long> categoryIds) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden reordenar categorías");
        }

        for (int i = 0; i < categoryIds.size(); i++) {
            final int index = i;
            Long categoryId = categoryIds.get(i);
            Categoria categoria =
                    categoriaRepository
                            .findById(categoryId)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Categoría no encontrada: " + categoryId));

            // Verificar que pertenezca a esta comunidad
            if (!categoria.getComunidad().getId().equals(communityId)) {
                throw new IllegalArgumentException(
                        "Una o más categorías no pertenecen a esta comunidad");
            }

            categoria.setOrden(index + 1);
            categoriaRepository.save(categoria);
        }

        return categoriaRepository.findByComunidadIdOrderByOrden(communityId);
    }
}
