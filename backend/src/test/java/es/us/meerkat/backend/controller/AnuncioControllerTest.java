package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.controller.communities.AnuncioController;
import es.us.meerkat.backend.dto.communities.AnuncioListResponse;
import es.us.meerkat.backend.dto.communities.AnuncioResponse;
import es.us.meerkat.backend.dto.communities.CreateAnuncioRequest;
import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.AnuncioService;

@ExtendWith(MockitoExtension.class)
class AnuncioControllerTest {

    @Mock private AnuncioService anuncioService;

    @InjectMocks private AnuncioController controller;

    @Test
    void listAnunciosShouldReturnOkWithPaginatedResults() {
        Anuncio anuncio = new Anuncio();
        anuncio.setId(1L);

        when(anuncioService.getAnunciosByCommunity(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(anuncio)));

        ResponseEntity<AnuncioListResponse> response = controller.listAnuncios(10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().anuncios()).hasSize(1);
    }

    @Test
    void createAnuncioShouldReturnCreated() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Anuncio anuncio = new Anuncio();
        anuncio.setId(100L);

        when(anuncioService.createAnuncio(eq(1L), eq(10L), any())).thenReturn(anuncio);

        CreateAnuncioRequest request =
                new CreateAnuncioRequest("Titulo test", "Contenido del anuncio", true);
        ResponseEntity<AnuncioResponse> response = controller.createAnuncio(10L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getAnuncioShouldReturnOk() {
        Anuncio anuncio = new Anuncio();
        anuncio.setId(100L);

        when(anuncioService.getAnuncioById(100L)).thenReturn(anuncio);

        ResponseEntity<AnuncioResponse> response = controller.getAnuncio(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
