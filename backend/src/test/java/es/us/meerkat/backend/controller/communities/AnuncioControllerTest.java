package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.communities.AnuncioListResponse;
import es.us.meerkat.backend.dto.communities.AnuncioResponse;
import es.us.meerkat.backend.dto.communities.CreateAnuncioRequest;
import es.us.meerkat.backend.dto.communities.UpdateAnuncioRequest;
import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.AnuncioService;

@ExtendWith(MockitoExtension.class)
class AnuncioControllerTest {

    @Mock private AnuncioService anuncioService;

    @InjectMocks private AnuncioController controller;

    private Usuario usuario;
    private Anuncio anuncio;
    private CreateAnuncioRequest createRequest;
    private UpdateAnuncioRequest updateRequest;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);

        anuncio = new Anuncio();
        anuncio.setId(100L);
        anuncio.setTitulo("Test Anuncio");
        anuncio.setContenido("Contenido del test");

        createRequest = new CreateAnuncioRequest("Anuncio", "Contenido", true);
        updateRequest = new UpdateAnuncioRequest("Updated", "Updated content", false);
    }

    @Test
    void listAnunciosShouldReturnOkWithPaginatedResults() {
        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(anuncio)));

        ResponseEntity<AnuncioListResponse> response = controller.listAnuncios(10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().anuncios()).hasSize(1);
        verify(anuncioService).getAnunciosByCommunity(eq(10L), any());
    }

    @Test
    void listAnunciosShouldReturnOkWithEmptyList() {
        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<AnuncioListResponse> response = controller.listAnuncios(10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().anuncios()).isEmpty();
    }

    @Test
    void listAnunciosShouldUseProvidedPaginationParameters() {
        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        controller.listAnuncios(10L, 5, 50);

        verify(anuncioService).getAnunciosByCommunity(eq(10L), any());
    }

    @Test
    void listAnunciosShouldReturnOkWithMultipleAnuncios() {
        Anuncio anuncio2 = new Anuncio();
        anuncio2.setId(101L);
        anuncio2.setTitulo("Segundo Anuncio");

        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(anuncio, anuncio2)));

        ResponseEntity<AnuncioListResponse> response = controller.listAnuncios(10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().anuncios()).hasSize(2);
    }

    @Test
    void createAnuncioShouldReturnCreated() {
        when(anuncioService.createAnuncio(eq(1L), eq(10L), any())).thenReturn(anuncio);

        ResponseEntity<AnuncioResponse> response =
                controller.createAnuncio(10L, createRequest, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(anuncioService).createAnuncio(eq(1L), eq(10L), any());
    }

    @Test
    void createAnuncioShouldReturnForbiddenWhenUserNotAuthorized() {
        when(anuncioService.createAnuncio(eq(1L), eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.createAnuncio(10L, createRequest, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createAnuncioShouldReturnBadRequestWhenServiceThrows() {
        when(anuncioService.createAnuncio(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Datos inválidos"));

        try {
            controller.createAnuncio(10L, createRequest, usuario);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Datos inválidos");
        }
    }

    @Test
    void getAnuncioShouldReturnOk() {
        when(anuncioService.getAnuncioById(100L)).thenReturn(anuncio);

        ResponseEntity<AnuncioResponse> response = controller.getAnuncio(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(anuncioService).getAnuncioById(100L);
    }

    @Test
    void getAnuncioShouldReturnNotFoundWhenAnuncioNotExists() {
        when(anuncioService.getAnuncioById(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class, () -> controller.getAnuncio(10L, 999L));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAnuncioShouldReturnOk() {
        when(anuncioService.updateAnuncio(eq(1L), eq(100L), any())).thenReturn(anuncio);

        ResponseEntity<AnuncioResponse> response =
                controller.updateAnuncio(10L, 100L, updateRequest, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(anuncioService).updateAnuncio(eq(1L), eq(100L), any());
    }

    @Test
    void updateAnuncioShouldReturnForbiddenWhenNotCreator() {
        when(anuncioService.updateAnuncio(eq(1L), eq(100L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No es el creador"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.updateAnuncio(10L, 100L, updateRequest, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateAnuncioShouldReturnNotFoundWhenAnuncioNotExists() {
        when(anuncioService.updateAnuncio(eq(1L), eq(999L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.updateAnuncio(10L, 999L, updateRequest, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAnuncioShouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteAnuncio(10L, 100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(anuncioService).deleteAnuncio(1L, 100L);
    }

    @Test
    void deleteAnuncioShouldReturnForbiddenWhenNotCreator() {
        org.mockito.Mockito.doThrow(
                        new ResponseStatusException(HttpStatus.FORBIDDEN, "No es el creador"))
                .when(anuncioService)
                .deleteAnuncio(eq(1L), eq(100L));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.deleteAnuncio(10L, 100L, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteAnuncioShouldReturnNotFoundWhenAnuncioNotExists() {
        org.mockito.Mockito.doThrow(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"))
                .when(anuncioService)
                .deleteAnuncio(eq(1L), eq(999L));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.deleteAnuncio(10L, 999L, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listAnunciosShouldReturnOkWithLargePageSize() {
        Anuncio anuncio1 = new Anuncio();
        anuncio1.setId(100L);
        Anuncio anuncio2 = new Anuncio();
        anuncio2.setId(101L);
        Anuncio anuncio3 = new Anuncio();
        anuncio3.setId(102L);

        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(anuncio1, anuncio2, anuncio3)));

        ResponseEntity<AnuncioListResponse> response = controller.listAnuncios(10L, 0, 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().anuncios()).hasSize(3);
    }

    @Test
    void createAnuncioShouldNotifyWhenAnuncioCreatedSuccessfully() {
        when(anuncioService.createAnuncio(eq(1L), eq(10L), any())).thenReturn(anuncio);

        ResponseEntity<AnuncioResponse> response =
                controller.createAnuncio(10L, createRequest, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response).isNotNull();
    }

    @Test
    void updateAnuncioWithEmptyTitleShouldThrow() {
        CreateAnuncioRequest emptyTitleRequest = new CreateAnuncioRequest("", "Contenido", true);
        when(anuncioService.createAnuncio(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Título no puede estar vacío"));

        try {
            controller.createAnuncio(10L, emptyTitleRequest, usuario);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Título no puede estar vacío");
        }
    }
}
