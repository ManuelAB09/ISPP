package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.dto.RecomendacionResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.FactorRecomendacion;
import es.us.meerkat.backend.entity.RecomendacionComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.RecomendacionComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecomendacionComunidadServiceTest {

    @Mock private RecomendacionComunidadRepository recomendacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadRepository comunidadRepository;

    @InjectMocks private RecomendacionComunidadService recomendacionService;

    @Test
    void crearRecomendacionShouldCreateSuccessfullyWhenNew() {
        Long usuarioId = 1L;
        Long comunidadId = 10L;
        Usuario usuario = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(comunidadId);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(recomendacionRepository.findByUsuarioAndComunidad(usuario, comunidad))
                .thenReturn(Optional.empty());
        when(recomendacionRepository.save(any(RecomendacionComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecomendacionComunidad result =
                recomendacionService.crearRecomendacion(
                        usuarioId, comunidadId, FactorRecomendacion.INTERES_SIMILAR, 85.5);

        assertThat(result.getUsuario()).isEqualTo(usuario);
        assertThat(result.getComunidad()).isEqualTo(comunidad);
        assertThat(result.getFactor()).isEqualTo(FactorRecomendacion.INTERES_SIMILAR);
        assertThat(result.getRelevancia()).isEqualTo(85.5);
        assertThat(result.getVista()).isFalse();
        verify(recomendacionRepository).save(result);
    }

    @Test
    void crearRecomendacionShouldUpdateWhenAlreadyExists() {
        Long usuarioId = 1L;
        Long comunidadId = 10L;
        Usuario usuario = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(comunidadId);

        // Create a real (not mocked) existing recommendation
        RecomendacionComunidad existing =
                RecomendacionComunidad.builder()
                        .id(1L)
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .factor(FactorRecomendacion.UBICACION)
                        .relevancia(50.0)
                        .vista(false)
                        .createdAt(LocalDateTime.now())
                        .build();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(recomendacionRepository.findByUsuarioAndComunidad(usuario, comunidad))
                .thenReturn(Optional.of(existing));
        when(recomendacionRepository.save(any(RecomendacionComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecomendacionComunidad result =
                recomendacionService.crearRecomendacion(
                        usuarioId, comunidadId, FactorRecomendacion.INTERES_SIMILAR, 85.5);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRelevancia()).isEqualTo(85.5);
        assertThat(result.getFactor()).isEqualTo(FactorRecomendacion.INTERES_SIMILAR);
    }

    @Test
    void getRecomendacionesUsuarioShouldReturnPagedRecommendations() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);

        RecomendacionComunidad rec1 =
                buildRecomendacion(
                        1L,
                        usuario,
                        buildComunidad(10L),
                        FactorRecomendacion.INTERES_SIMILAR,
                        90.0);
        RecomendacionComunidad rec2 =
                buildRecomendacion(
                        2L, usuario, buildComunidad(11L), FactorRecomendacion.UBICACION, 75.0);

        Page<RecomendacionComunidad> page =
                new PageImpl<>(java.util.List.of(rec1, rec2), PageRequest.of(0, 10), 2);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(recomendacionRepository.findByUsuarioOrderByRelevanciaDesc(
                        usuario, page.getPageable()))
                .thenReturn(page);

        Page<RecomendacionComunidad> result =
                recomendacionService.getRecomendacionesUsuario(usuarioId, page.getPageable());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getRelevancia()).isEqualTo(90.0);
        assertThat(result.getContent().get(1).getRelevancia()).isEqualTo(75.0);
    }

    @Test
    void getRecomendacionesNoVistasShouldReturnOnlyUnviewedRecommendations() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);

        RecomendacionComunidad rec1 =
                buildRecomendacion(
                        1L,
                        usuario,
                        buildComunidad(10L),
                        FactorRecomendacion.INTERES_SIMILAR,
                        90.0);
        rec1.setVista(false);

        RecomendacionComunidad rec2 =
                buildRecomendacion(
                        2L, usuario, buildComunidad(11L), FactorRecomendacion.UBICACION, 75.0);
        rec2.setVista(false);

        Page<RecomendacionComunidad> page =
                new PageImpl<>(java.util.List.of(rec1, rec2), PageRequest.of(0, 10), 2);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(recomendacionRepository.findByUsuarioAndVistaFalseOrderByRelevanciaDesc(
                        usuario, page.getPageable()))
                .thenReturn(page);

        Page<RecomendacionComunidad> result =
                recomendacionService.getRecomendacionesNoVistas(usuarioId, page.getPageable());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(r -> !r.getVista());
    }

    @Test
    void marcarComoVistaShouldMarkRecommendationAsViewed() {
        Long recomendacionId = 1L;
        RecomendacionComunidad recomendacion =
                buildRecomendacion(
                        recomendacionId,
                        buildUsuario(1L),
                        buildComunidad(10L),
                        FactorRecomendacion.INTERES_SIMILAR,
                        85.0);
        recomendacion.setVista(false);

        when(recomendacionRepository.findById(recomendacionId))
                .thenReturn(Optional.of(recomendacion));
        when(recomendacionRepository.save(any(RecomendacionComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        recomendacionService.marcarComoVista(recomendacionId);

        assertThat(recomendacion.getVista()).isTrue();
        assertThat(recomendacion.getFechaVista()).isNotNull();
        verify(recomendacionRepository).save(recomendacion);
    }

    @Test
    void marcarComoVistaShouldThrowWhenRecommendationNotFound() {
        Long recomendacionId = 999L;
        when(recomendacionRepository.findById(recomendacionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recomendacionService.marcarComoVista(recomendacionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recomendación no encontrada");
    }

    @Test
    void eliminarRecomendacionShouldDeleteSuccessfully() {
        Long recomendacionId = 1L;

        recomendacionService.eliminarRecomendacion(recomendacionId);

        verify(recomendacionRepository).deleteById(recomendacionId);
    }

    @Test
    void getRecomendacionByIdShouldReturnRecommendation() {
        Long recomendacionId = 1L;
        RecomendacionComunidad recomendacion =
                buildRecomendacion(
                        recomendacionId,
                        buildUsuario(1L),
                        buildComunidad(10L),
                        FactorRecomendacion.INTERES_SIMILAR,
                        85.0);

        when(recomendacionRepository.findById(recomendacionId))
                .thenReturn(Optional.of(recomendacion));

        RecomendacionComunidad result = recomendacionService.getRecomendacionById(recomendacionId);

        assertThat(result.getId()).isEqualTo(recomendacionId);
        assertThat(result.getRelevancia()).isEqualTo(85.0);
    }

    @Test
    void toResponseShouldConvertRecomendacionToDTOCorrectly() {
        RecomendacionComunidad recomendacion =
                buildRecomendacion(
                        1L,
                        buildUsuario(1L),
                        buildComunidad(10L),
                        FactorRecomendacion.INTERES_SIMILAR,
                        85.0);

        RecomendacionResponse response = recomendacionService.toResponse(recomendacion);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCommunityId()).isEqualTo(10L);
        assertThat(response.getFactor()).isEqualTo(FactorRecomendacion.INTERES_SIMILAR);
        assertThat(response.getRelevancia()).isEqualTo(85.0);
        assertThat(response.getMotivo()).isNotBlank();
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        usuario.setFoto("https://avatar.com/user" + id + ".jpg");
        return usuario;
    }

    private Comunidad buildComunidad(final Long id) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad " + id)
                .descripcion("Descripción")
                .imagenUrl("https://img.com/comunidad" + id + ".jpg")
                .creador(buildUsuario(1L))
                .build();
    }

    private RecomendacionComunidad buildRecomendacion(
            final Long id,
            final Usuario usuario,
            final Comunidad comunidad,
            final FactorRecomendacion factor,
            final Double relevancia) {
        return RecomendacionComunidad.builder()
                .id(id)
                .usuario(usuario)
                .comunidad(comunidad)
                .factor(factor)
                .relevancia(relevancia)
                .vista(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
