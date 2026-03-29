package es.us.meerkat.backend.service.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.communities.Categoria;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.repository.communities.CategoriaRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private CategoryService categoryService;

    // ================================================================
    // createCategory
    // ================================================================

    @Test
    void createCategoryShouldAssignNextOrder() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Comunidad").build();

        Categoria existing =
                Categoria.builder()
                        .id(1L)
                        .nombre("Matemáticas")
                        .orden(2)
                        .comunidad(comunidad)
                        .build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(categoriaRepository.findByComunidadIdOrderByOrden(communityId))
                .thenReturn(List.of(existing));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Categoria created =
                categoryService.createCategory(userId, communityId, "Historia", "Debates");

        assertThat(created.getNombre()).isEqualTo("Historia");
        assertThat(created.getOrden()).isEqualTo(3);
        verify(categoriaRepository).save(created);
    }

    @Test
    void createCategoryShouldAssignOrder1WhenEmpty() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Comunidad").build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(categoriaRepository.findByComunidadIdOrderByOrden(communityId)).thenReturn(List.of());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria created = categoryService.createCategory(userId, communityId, "Primera", "Desc");

        assertThat(created.getOrden()).isEqualTo(1);
    }

    @Test
    void createCategoryShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.createCategory(1L, 10L, "Cat", "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo admins");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void createCategoryShouldThrowWhenCommunityNotFound() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(1L, 10L, "Cat", "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comunidad no encontrada");
    }

    // ================================================================
    // updateCategory
    // ================================================================

    @Test
    void updateCategoryShouldFailWhenCategoryBelongsToOtherCommunity() {
        Long userId = 1L;
        Long communityId = 10L;

        Comunidad otherCommunity = Comunidad.builder().id(999L).nombre("Otra").build();
        Categoria categoria =
                Categoria.builder()
                        .id(77L)
                        .nombre("Cat")
                        .comunidad(otherCommunity)
                        .orden(1)
                        .build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(categoriaRepository.findById(77L)).thenReturn(Optional.of(categoria));

        assertThatThrownBy(
                        () ->
                                categoryService.updateCategory(
                                        userId, communityId, 77L, "Nuevo", "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void updateCategoryShouldUpdateNameAndDescription() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Com").build();
        Categoria categoria =
                Categoria.builder()
                        .id(1L)
                        .nombre("Old")
                        .descripcion("OldDesc")
                        .comunidad(comunidad)
                        .orden(1)
                        .build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Categoria updated =
                categoryService.updateCategory(userId, communityId, 1L, "New", "NewDesc");

        assertThat(updated.getNombre()).isEqualTo("New");
        assertThat(updated.getDescripcion()).isEqualTo("NewDesc");
    }

    @Test
    void updateCategoryShouldNotUpdateNameWhenBlank() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Com").build();
        Categoria categoria =
                Categoria.builder()
                        .id(1L)
                        .nombre("Keep")
                        .descripcion("OldDesc")
                        .comunidad(comunidad)
                        .orden(1)
                        .build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Categoria updated =
                categoryService.updateCategory(userId, communityId, 1L, "  ", "NewDesc");

        assertThat(updated.getNombre()).isEqualTo("Keep");
        assertThat(updated.getDescripcion()).isEqualTo("NewDesc");
    }

    @Test
    void updateCategoryShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, 10L, 1L, "N", "D"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateCategoryShouldThrowWhenNotFound() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(1L, 10L, 1L, "N", "D"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoría no encontrada");
    }

    // ================================================================
    // deleteCategory
    // ================================================================

    @Test
    void deleteCategoryShouldDeleteWhenValid() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Com").build();
        Categoria categoria = Categoria.builder().id(1L).comunidad(comunidad).orden(1).build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        categoryService.deleteCategory(userId, communityId, 1L);

        verify(categoriaRepository).delete(categoria);
    }

    @Test
    void deleteCategoryShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L, 10L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteCategoryShouldThrowWhenWrongCommunity() {
        Comunidad otherCom = Comunidad.builder().id(99L).nombre("Other").build();
        Categoria categoria = Categoria.builder().id(1L).comunidad(otherCom).orden(1).build();

        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        assertThatThrownBy(() -> categoryService.deleteCategory(1L, 10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    // ================================================================
    // listCategories
    // ================================================================

    @Test
    void listCategoriesShouldReturnOrdered() {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("Com").build();
        Categoria c1 = Categoria.builder().id(1L).nombre("A").orden(1).comunidad(comunidad).build();
        Categoria c2 = Categoria.builder().id(2L).nombre("B").orden(2).comunidad(comunidad).build();

        when(categoriaRepository.findByComunidadIdOrderByOrden(10L)).thenReturn(List.of(c1, c2));

        List<Categoria> result = categoryService.listCategories(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNombre()).isEqualTo("A");
    }

    // ================================================================
    // reorderCategories
    // ================================================================

    @Test
    void reorderCategoriesShouldPersistSequentialOrder() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = Comunidad.builder().id(communityId).nombre("Comunidad").build();

        Categoria c1 =
                Categoria.builder().id(101L).nombre("A").orden(5).comunidad(comunidad).build();
        Categoria c2 =
                Categoria.builder().id(102L).nombre("B").orden(9).comunidad(comunidad).build();

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(categoriaRepository.findById(101L)).thenReturn(Optional.of(c1));
        when(categoriaRepository.findById(102L)).thenReturn(Optional.of(c2));
        when(categoriaRepository.findByComunidadIdOrderByOrden(communityId))
                .thenReturn(List.of(c1, c2));

        List<Categoria> result =
                categoryService.reorderCategories(userId, communityId, List.of(102L, 101L));

        assertThat(c2.getOrden()).isEqualTo(1);
        assertThat(c1.getOrden()).isEqualTo(2);
        assertThat(result).hasSize(2);
        verify(categoriaRepository).save(c2);
        verify(categoriaRepository).save(c1);
    }

    @Test
    void reorderCategoriesShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.reorderCategories(1L, 10L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reorderCategoriesShouldThrowWhenCategoryFromOtherCommunity() {
        Comunidad otherCom = Comunidad.builder().id(99L).nombre("Other").build();
        Categoria c1 = Categoria.builder().id(1L).comunidad(otherCom).orden(1).build();

        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c1));

        assertThatThrownBy(() -> categoryService.reorderCategories(1L, 10L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenecen");
    }
}
