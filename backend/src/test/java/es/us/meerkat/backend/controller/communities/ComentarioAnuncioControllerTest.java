package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.assertThat;
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

import es.us.meerkat.backend.controller.forms.ComentarioAnuncioController;
import es.us.meerkat.backend.dto.communities.ComentarioAnuncioResponse;
import es.us.meerkat.backend.service.forms.ComentarioAnuncioService;

@ExtendWith(MockitoExtension.class)
class ComentarioAnuncioControllerTest {

    @Mock private ComentarioAnuncioService comentarioService;

    @InjectMocks private ComentarioAnuncioController controller;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void listarComentariosShouldReturnOk() {
        when(comentarioService.listarComentarios(1L)).thenReturn(List.of());

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
                        mock(ComentarioAnuncioResponse.class));

        when(comentarioService.listarComentarios(1L)).thenReturn(comments);

        ResponseEntity<List<ComentarioAnuncioResponse>> response = controller.listarComentarios(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void listarComentariosShouldReturnEmptyListWhenNoComments() {
        when(comentarioService.listarComentarios(999L)).thenReturn(List.of());

        ResponseEntity<List<ComentarioAnuncioResponse>> response =
                controller.listarComentarios(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
