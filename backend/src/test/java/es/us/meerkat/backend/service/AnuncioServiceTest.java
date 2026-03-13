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

import es.us.meerkat.backend.dto.AnuncioResponse;
import es.us.meerkat.backend.dto.CreateAnuncioRequest;
import es.us.meerkat.backend.dto.UpdateAnuncioRequest;
import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AnuncioRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

    @Mock private AnuncioRepository anuncioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private AnuncioService anuncioService;

    @Test
    void createAnuncioShouldCreateSuccessfullyWhenAdminRequestsIt() {
        Long userId = 1L;
        Long communityId = 10L;
        Usuario usuario = buildUsuario(userId);
        Comunidad comunidad = buildComunidad(communityId);
        CreateAnuncioRequest request =
                new CreateAnuncioRequest("Anuncio Title", "Contenido del anuncio", true);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(anuncioRepository.save(any(Anuncio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Anuncio result = anuncioService.createAnuncio(userId, communityId, request);

        assertThat(result.getTitulo()).isEqualTo("Anuncio Title");
        assertThat(result.getContenido()).isEqualTo("Contenido del anuncio");
        assertThat(result.getPermitirComentarios()).isTrue();
        assertThat(result.getUsuario()).isEqualTo(usuario);
        assertThat(result.getComunidad()).isEqualTo(comunidad);
        verify(anuncioRepository).save(result);
    }

    @Test
    void createAnuncioShouldFailWhenUserIsNotAdmin() {
        Long userId = 1L;
        Long communityId = 10L;
        CreateAnuncioRequest request =
                new CreateAnuncioRequest("Anuncio Title", "Contenido del anuncio", true);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(false);

        assertThatThrownBy(() -> anuncioService.createAnuncio(userId, communityId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo administradores");
    }

    @Test
    void getAnunciosByCommunityShouldReturnPagedAnnouncements() {
        Long communityId = 10L;
        Comunidad comunidad = buildComunidad(communityId);
        Usuario usuario = buildUsuario(1L);

        Anuncio anuncio1 =
                Anuncio.builder()
                        .id(1L)
                        .titulo("Anuncio 1")
                        .contenido("Contenido 1")
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .permitirComentarios(true)
                        .build();

        Anuncio anuncio2 =
                Anuncio.builder()
                        .id(2L)
                        .titulo("Anuncio 2")
                        .contenido("Contenido 2")
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .permitirComentarios(false)
                        .build();

        Page<Anuncio> page =
                new PageImpl<>(java.util.List.of(anuncio1, anuncio2), PageRequest.of(0, 10), 2);

        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(anuncioRepository.findByComunidadOrderByCreatedAtDesc(comunidad, page.getPageable()))
                .thenReturn(page);

        Page<Anuncio> result =
                anuncioService.getAnunciosByCommunity(communityId, page.getPageable());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitulo()).isEqualTo("Anuncio 1");
        assertThat(result.getContent().get(1).getTitulo()).isEqualTo("Anuncio 2");
    }

    @Test
    void getAnuncioByIdShouldReturnAnuncioWhenExists() {
        Long anuncioId = 100L;
        Anuncio anuncio = buildAnuncio(anuncioId);

        when(anuncioRepository.findById(anuncioId)).thenReturn(Optional.of(anuncio));

        Anuncio result = anuncioService.getAnuncioById(anuncioId);

        assertThat(result.getId()).isEqualTo(anuncioId);
        assertThat(result.getTitulo()).isEqualTo("Test Anuncio");
    }

    @Test
    void getAnuncioByIdShouldThrowWhenNotExists() {
        Long anuncioId = 999L;
        when(anuncioRepository.findById(anuncioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anuncioService.getAnuncioById(anuncioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Anuncio no encontrado");
    }

    @Test
    void updateAnuncioShouldUpdateWhenAdminRequest() {
        Long userId = 1L;
        Long anuncioId = 100L;
        Anuncio anuncio = buildAnuncio(anuncioId);
        UpdateAnuncioRequest request =
                new UpdateAnuncioRequest("Nuevo título", "Nuevo contenido", false);

        when(anuncioRepository.findById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(authorizationService.isAdminOf(userId, anuncio.getComunidad().getId()))
                .thenReturn(true);
        when(anuncioRepository.save(any(Anuncio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Anuncio result = anuncioService.updateAnuncio(userId, anuncioId, request);

        assertThat(result.getTitulo()).isEqualTo("Nuevo título");
        assertThat(result.getContenido()).isEqualTo("Nuevo contenido");
        assertThat(result.getPermitirComentarios()).isFalse();
        verify(anuncioRepository).save(result);
    }

    @Test
    void updateAnuncioShouldFailWhenUserNotAuthorized() {
        Long userId = 1L;
        Long anuncioId = 100L;
        Anuncio anuncio = buildAnuncio(anuncioId);
        anuncio.setUsuario(buildUsuario(999L)); // Diferente usuario
        UpdateAnuncioRequest request =
                new UpdateAnuncioRequest("Nuevo título", "Nuevo contenido", true);

        when(anuncioRepository.findById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(authorizationService.isAdminOf(userId, anuncio.getComunidad().getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> anuncioService.updateAnuncio(userId, anuncioId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    void deleteAnuncioByShouldDeleteWhenAdminRequest() {
        Long userId = 1L;
        Long anuncioId = 100L;
        Anuncio anuncio = buildAnuncio(anuncioId);

        when(anuncioRepository.findById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(authorizationService.isAdminOf(userId, anuncio.getComunidad().getId()))
                .thenReturn(true);

        anuncioService.deleteAnuncio(userId, anuncioId);

        verify(anuncioRepository).delete(anuncio);
    }

    @Test
    void toResponseShouldConvertAnuncioToDTOCorrectly() {
        Anuncio anuncio = buildAnuncio(1L);

        AnuncioResponse response = anuncioService.toResponse(anuncio);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.titulo()).isEqualTo("Test Anuncio");
        assertThat(response.contenido()).isEqualTo("Test content");
        assertThat(response.usuario().nombre()).isEqualTo("Usuario 1");
        assertThat(response.permitirComentarios()).isTrue();
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
                .creador(buildUsuario(1L))
                .build();
    }

    private Anuncio buildAnuncio(final Long id) {
        return Anuncio.builder()
                .id(id)
                .titulo("Test Anuncio")
                .contenido("Test content")
                .usuario(buildUsuario(1L))
                .comunidad(buildComunidad(10L))
                .permitirComentarios(true)
                .editado(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
