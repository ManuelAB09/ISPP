package es.us.meerkat.backend.service.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
