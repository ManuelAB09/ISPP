package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.controller.forms.ComentarioAnuncioController;
import es.us.meerkat.backend.dto.communities.ComentarioAnuncioResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.forms.ComentarioAnuncioService;

@ExtendWith(MockitoExtension.class)
class ComentarioAnuncioControllerTest {

    @Mock private ComentarioAnuncioService comentarioService;

    @InjectMocks private ComentarioAnuncioController controller;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario =
                Usuario.builder().id(1L).nombre("Admin").email("admin@t.com").password("p").build();
    }

    // ================================================================
    // listarComentarios
    // ================================================================

    @Test
    void listarComentariosShouldReturnOk() {
        when(comentarioService.listarComentarios(1L)).thenReturn(List.of());

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(comentarioService).listarComentarios(1L);
    }

    @Test
    void listarComentariosShouldDelegateToServiceWithAnuncioId() {
        when(comentarioService.listarComentarios(5L)).thenReturn(List.of());

        controller.listarComentarios(5L);

        verify(comentarioService).listarComentarios(5L);
    }

    @Test
    void listarComentariosShouldReturnMultipleComments() {
        List<ComentarioAnuncioResponse> comments =
                List.of(
                        mock(ComentarioAnuncioResponse.class),
                        mock(ComentarioAnuncioResponse.class),
                        mock(ComentarioAnuncioResponse.class));

        when(comentarioService.listarComentarios(1L)).thenReturn(comments);

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void listarComentariosShouldReturnEmptyListWhenNoComments() {
        when(comentarioService.listarComentarios(999L)).thenReturn(List.of());

        ResponseEntity<List<ComentarioAnuncioResponse>> response =
                controller.listarComentarios(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void listarComentariosShouldReturnOkWithSingleComment() {
        ComentarioAnuncioResponse comment = mock(ComentarioAnuncioResponse.class);
        when(comentarioService.listarComentarios(2L)).thenReturn(List.of(comment));

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(comentarioService).listarComentarios(2L);
    }

    @Test
    void listarComentariosShouldThrowWhenAnuncioNotFound() {
        when(comentarioService.listarComentarios(999L))
                .thenThrow(new IllegalArgumentException("Anuncio no encontrado"));

        try {
            controller.listarComentarios(999L);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Anuncio no encontrado");
        }
    }

    @Test
    void listarComentariosShouldReturnOkWithOrderedComments() {
        ComentarioAnuncioResponse comment1 = mock(ComentarioAnuncioResponse.class);
        ComentarioAnuncioResponse comment2 = mock(ComentarioAnuncioResponse.class);
        when(comentarioService.listarComentarios(3L)).thenReturn(List.of(comment1, comment2));

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(comment1, comment2);
    }

    // ================================================================
    // eliminarComentario
    // ================================================================

    @Test
    void eliminarComentarioShouldReturnNoContent() {
        doNothing().when(comentarioService).eliminarComentario(10L, usuario.getId());

        ResponseEntity<Void> response = controller.eliminarComentario(1L, 10L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(comentarioService).eliminarComentario(10L, usuario.getId());
    }

    @Test
    void eliminarComentarioShouldReturnUnauthorizedWhenNoUser() {
        ResponseEntity<Void> response = controller.eliminarComentario(1L, 10L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eliminarComentarioShouldReturnForbiddenWhenNoPermission() {
        doThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.FORBIDDEN, "Sin permisos"))
                .when(comentarioService)
                .eliminarComentario(10L, usuario.getId());

        ResponseEntity<Void> response = controller.eliminarComentario(1L, 10L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eliminarComentarioShouldReturnNotFoundWhenCommentNotFound() {
        doThrow(new IllegalArgumentException("Comentario no encontrado"))
                .when(comentarioService)
                .eliminarComentario(99L, usuario.getId());

        ResponseEntity<Void> response = controller.eliminarComentario(1L, 99L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eliminarComentarioDelegatesCorrectCommentIdToService() {
        doNothing().when(comentarioService).eliminarComentario(55L, usuario.getId());

        controller.eliminarComentario(1L, 55L, usuario);

        verify(comentarioService).eliminarComentario(55L, usuario.getId());
    }
}
