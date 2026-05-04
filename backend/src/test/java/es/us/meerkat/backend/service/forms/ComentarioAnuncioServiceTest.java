package es.us.meerkat.backend.service.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.communities.ComentarioAnuncioResponse;
import es.us.meerkat.backend.dto.communities.CreateComentarioAnuncioRequest;
import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.communities.ComentarioAnuncio;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.AnuncioRepository;
import es.us.meerkat.backend.repository.communities.ComentarioAnuncioRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;

@ExtendWith(MockitoExtension.class)
class ComentarioAnuncioServiceTest {

    @Mock private ComentarioAnuncioRepository comentarioRepo;
    @Mock private AnuncioRepository anuncioRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private ComentarioAnuncioService comentarioAnuncioService;

    // ================================================================
    // crearComentario
    // ================================================================

    @Test
    void crearComentarioShouldSaveAndReturnResponse() {
        Anuncio anuncio = Anuncio.builder().id(1L).titulo("Anuncio").contenido("Contenido").build();
        Usuario usuario =
                Usuario.builder()
                        .id(2L)
                        .nombre("Juan")
                        .email("j@t.com")
                        .foto("foto.png")
                        .password("p")
                        .build();

        when(anuncioRepo.findById(1L)).thenReturn(Optional.of(anuncio));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(usuario));

        ComentarioAnuncio saved =
                ComentarioAnuncio.builder()
                        .id(10L)
                        .anuncio(anuncio)
                        .usuario(usuario)
                        .texto("Buen anuncio")
                        .createdAt(LocalDateTime.now())
                        .build();
        when(comentarioRepo.save(any(ComentarioAnuncio.class))).thenReturn(saved);

        CreateComentarioAnuncioRequest req = new CreateComentarioAnuncioRequest("Buen anuncio");

        ComentarioAnuncioResponse response = comentarioAnuncioService.crearComentario(1L, 2L, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.texto()).isEqualTo("Buen anuncio");
        assertThat(response.usuario().id()).isEqualTo(2L);
        assertThat(response.usuario().nombre()).isEqualTo("Juan");
        assertThat(response.usuario().email()).isEqualTo("j@t.com");
        assertThat(response.usuario().avatarUrl()).isEqualTo("foto.png");
    }

    @Test
    void crearComentarioShouldThrowWhenAnuncioNotFound() {
        when(anuncioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                comentarioAnuncioService.crearComentario(
                                        99L, 1L, new CreateComentarioAnuncioRequest("texto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anuncio no encontrado");
    }

    @Test
    void crearComentarioShouldThrowWhenUsuarioNotFound() {
        Anuncio anuncio = Anuncio.builder().id(1L).titulo("A").contenido("C").build();
        when(anuncioRepo.findById(1L)).thenReturn(Optional.of(anuncio));
        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                comentarioAnuncioService.crearComentario(
                                        1L, 99L, new CreateComentarioAnuncioRequest("texto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ================================================================
    // listarComentarios
    // ================================================================

    @Test
    void listarComentariosShouldReturnListOfResponses() {
        Anuncio anuncio = Anuncio.builder().id(1L).titulo("A").contenido("C").build();
        Usuario u1 =
                Usuario.builder()
                        .id(1L)
                        .nombre("Ana")
                        .email("a@t.com")
                        .foto("f1.png")
                        .password("p")
                        .build();
        Usuario u2 =
                Usuario.builder()
                        .id(2L)
                        .nombre("Bob")
                        .email("b@t.com")
                        .foto(null)
                        .password("p")
                        .build();

        ComentarioAnuncio c1 =
                ComentarioAnuncio.builder()
                        .id(10L)
                        .anuncio(anuncio)
                        .usuario(u1)
                        .texto("Comentario 1")
                        .createdAt(LocalDateTime.now())
                        .build();
        ComentarioAnuncio c2 =
                ComentarioAnuncio.builder()
                        .id(11L)
                        .anuncio(anuncio)
                        .usuario(u2)
                        .texto("Comentario 2")
                        .createdAt(LocalDateTime.now())
                        .build();

        when(anuncioRepo.findById(1L)).thenReturn(Optional.of(anuncio));
        when(comentarioRepo.findByAnuncioOrderByCreatedAtDesc(anuncio)).thenReturn(List.of(c1, c2));

        List<ComentarioAnuncioResponse> responses = comentarioAnuncioService.listarComentarios(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).texto()).isEqualTo("Comentario 1");
        assertThat(responses.get(1).texto()).isEqualTo("Comentario 2");
    }

    @Test
    void listarComentariosShouldThrowWhenAnuncioNotFound() {
        when(anuncioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comentarioAnuncioService.listarComentarios(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anuncio no encontrado");
    }

    @Test
    void listarComentariosShouldReturnEmptyListWhenNoComments() {
        Anuncio anuncio = Anuncio.builder().id(1L).titulo("A").contenido("C").build();
        when(anuncioRepo.findById(1L)).thenReturn(Optional.of(anuncio));
        when(comentarioRepo.findByAnuncioOrderByCreatedAtDesc(anuncio)).thenReturn(List.of());

        List<ComentarioAnuncioResponse> responses = comentarioAnuncioService.listarComentarios(1L);

        assertThat(responses).isEmpty();
    }

    // ================================================================
    // toResponse (tested indirectly but verifying edge cases)
    // ================================================================

    @Test
    void toResponseShouldHandleNullFoto() {
        Usuario u =
                Usuario.builder()
                        .id(1L)
                        .nombre("X")
                        .email("x@t.com")
                        .foto(null)
                        .password("p")
                        .build();
        ComentarioAnuncio c =
                ComentarioAnuncio.builder()
                        .id(1L)
                        .usuario(u)
                        .texto("T")
                        .createdAt(LocalDateTime.now())
                        .anuncio(Anuncio.builder().id(1L).titulo("A").contenido("C").build())
                        .build();

        ComentarioAnuncioResponse response = comentarioAnuncioService.toResponse(c);

        assertThat(response.usuario().avatarUrl()).isNull();
    }

    // ================================================================
    // eliminarComentario
    // ================================================================

    @Test
    void eliminarComentarioByAuthorShouldDelete() {
        Comunidad comunidad = Comunidad.builder().id(1L).build();
        Anuncio anuncio =
                Anuncio.builder().id(1L).titulo("A").contenido("C").comunidad(comunidad).build();
        Usuario autor =
                Usuario.builder().id(10L).nombre("Autor").email("a@t.com").password("p").build();
        ComentarioAnuncio comentario =
                ComentarioAnuncio.builder()
                        .id(5L)
                        .anuncio(anuncio)
                        .usuario(autor)
                        .texto("Mi texto")
                        .createdAt(LocalDateTime.now())
                        .build();

        when(comentarioRepo.findById(5L)).thenReturn(Optional.of(comentario));
        when(authorizationService.isAdminOf(10L, 1L)).thenReturn(false);

        comentarioAnuncioService.eliminarComentario(5L, 10L);

        verify(comentarioRepo).delete(comentario);
    }

    @Test
    void eliminarComentarioByAdminShouldDelete() {
        Comunidad comunidad = Comunidad.builder().id(1L).build();
        Anuncio anuncio =
                Anuncio.builder().id(1L).titulo("A").contenido("C").comunidad(comunidad).build();
        Usuario autor =
                Usuario.builder().id(10L).nombre("Autor").email("a@t.com").password("p").build();
        ComentarioAnuncio comentario =
                ComentarioAnuncio.builder()
                        .id(5L)
                        .anuncio(anuncio)
                        .usuario(autor)
                        .texto("Texto")
                        .createdAt(LocalDateTime.now())
                        .build();

        when(comentarioRepo.findById(5L)).thenReturn(Optional.of(comentario));
        when(authorizationService.isAdminOf(99L, 1L)).thenReturn(true);

        comentarioAnuncioService.eliminarComentario(5L, 99L);

        verify(comentarioRepo).delete(comentario);
    }

    @Test
    void eliminarComentarioShouldThrowForbiddenWhenNotAuthorNorAdmin() {
        Comunidad comunidad = Comunidad.builder().id(1L).build();
        Anuncio anuncio =
                Anuncio.builder().id(1L).titulo("A").contenido("C").comunidad(comunidad).build();
        Usuario autor =
                Usuario.builder().id(10L).nombre("Autor").email("a@t.com").password("p").build();
        ComentarioAnuncio comentario =
                ComentarioAnuncio.builder()
                        .id(5L)
                        .anuncio(anuncio)
                        .usuario(autor)
                        .texto("Texto")
                        .createdAt(LocalDateTime.now())
                        .build();

        when(comentarioRepo.findById(5L)).thenReturn(Optional.of(comentario));
        when(authorizationService.isAdminOf(77L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> comentarioAnuncioService.eliminarComentario(5L, 77L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    void eliminarComentarioShouldThrowWhenNotFound() {
        when(comentarioRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comentarioAnuncioService.eliminarComentario(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comentario no encontrado");
    }
}
