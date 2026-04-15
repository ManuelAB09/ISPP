package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.communities.ApunteResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.ApunteService;

@ExtendWith(MockitoExtension.class)
class ApunteControllerTest {

    @Mock private ApunteService apunteService;
    @Mock private MultipartFile file;

    @InjectMocks private ApunteController controller;

    private Usuario usuario;
    private ApunteResponse apunteResponse;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);

        apunteResponse =
                ApunteResponse.builder()
                        .id(100L)
                        .titulo("Tema 1")
                        .descripcion("Resumen del tema 1")
                        .nombreArchivo("tema1.pdf")
                        .tipoMime("application/pdf")
                        .tamanioArchivo(2048L)
                        .usuarioId(1L)
                        .usuarioNombre("Usuario 1")
                        .comunidadId(10L)
                        .build();
    }

    @Test
    void subirApunteShouldReturnCreatedWhenRequestIsValid() {
        when(apunteService.subirApunte(eq(10L), eq(1L), eq("Tema 1"), eq("Resumen"), any()))
                .thenReturn(apunteResponse);

        ResponseEntity<ApunteResponse> response =
                controller.subirApunte(10L, usuario, file, "Tema 1", "Resumen");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(100L);
        verify(apunteService).subirApunte(10L, 1L, "Tema 1", "Resumen", file);
    }

    @Test
    void subirApunteShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<ApunteResponse> response =
                controller.subirApunte(10L, null, file, "Tema 1", "Resumen");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void subirApunteShouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() {
        when(apunteService.subirApunte(eq(10L), eq(1L), eq("Tema 1"), eq("Resumen"), any()))
                .thenThrow(new IllegalArgumentException("El archivo no puede estar vacio"));

        ResponseEntity<ApunteResponse> response =
                controller.subirApunte(10L, usuario, file, "Tema 1", "Resumen");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void subirApunteShouldAllowOptionalTitleAndDescription() {
        when(apunteService.subirApunte(eq(10L), eq(1L), eq(null), eq(null), any()))
                .thenReturn(apunteResponse);

        ResponseEntity<ApunteResponse> response =
                controller.subirApunte(10L, usuario, file, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(apunteService).subirApunte(10L, 1L, null, null, file);
    }

    @Test
    void obtenerApuntesShouldReturnOkWhenCommunityExists() {
        Page<ApunteResponse> page = new PageImpl<>(java.util.List.of(apunteResponse));
        when(apunteService.obtenerApuntesComunidad(eq(10L), any())).thenReturn(page);

        ResponseEntity<Page<ApunteResponse>> response = controller.obtenerApuntes(10L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(apunteService).obtenerApuntesComunidad(eq(10L), any());
    }

    @Test
    void obtenerApuntesShouldReturnNotFoundWhenServiceThrows() {
        when(apunteService.obtenerApuntesComunidad(eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Comunidad no encontrada"));

        ResponseEntity<Page<ApunteResponse>> response = controller.obtenerApuntes(10L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerApunteShouldReturnOkWhenItBelongsToCommunity() {
        when(apunteService.obtenerApunte(100L)).thenReturn(apunteResponse);

        ResponseEntity<ApunteResponse> response = controller.obtenerApunte(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(apunteResponse);
    }

    @Test
    void obtenerApunteShouldReturnNotFoundWhenItBelongsToAnotherCommunity() {
        ApunteResponse otro = ApunteResponse.builder().id(100L).comunidadId(99L).build();
        when(apunteService.obtenerApunte(100L)).thenReturn(otro);

        ResponseEntity<ApunteResponse> response = controller.obtenerApunte(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void descargarApunteShouldReturnOkWithFileHeaders() {
        byte[] contenido = "pdf".getBytes();
        when(apunteService.obtenerApunte(100L)).thenReturn(apunteResponse);
        when(apunteService.descargarApunte(100L)).thenReturn(contenido);

        ResponseEntity<byte[]> response = controller.descargarApunte(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contenido);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("tema1.pdf");
    }

    @Test
    void descargarApunteShouldReturnNotFoundWhenCommunityDoesNotMatch() {
        ApunteResponse otro =
                ApunteResponse.builder()
                        .id(100L)
                        .comunidadId(99L)
                        .nombreArchivo("tema1.pdf")
                        .tipoMime("application/pdf")
                        .build();
        when(apunteService.obtenerApunte(100L)).thenReturn(otro);

        ResponseEntity<byte[]> response = controller.descargarApunte(10L, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarApuntesShouldReturnOkWithResults() {
        Page<ApunteResponse> page = new PageImpl<>(java.util.List.of(apunteResponse));
        when(apunteService.buscarApuntes(eq(10L), eq("Tema"), any())).thenReturn(page);

        ResponseEntity<Page<ApunteResponse>> response =
                controller.buscarApuntes(10L, "Tema", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void buscarApuntesShouldReturnNotFoundWhenServiceThrows() {
        when(apunteService.buscarApuntes(eq(10L), eq("Tema"), any()))
                .thenThrow(new IllegalArgumentException("Comunidad no encontrada"));

        ResponseEntity<Page<ApunteResponse>> response =
                controller.buscarApuntes(10L, "Tema", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eliminarApunteShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Void> response = controller.eliminarApunte(10L, 100L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eliminarApunteShouldReturnNoContentWhenUserOwnsApunte() {
        when(apunteService.obtenerApunte(100L)).thenReturn(apunteResponse);

        ResponseEntity<Void> response = controller.eliminarApunte(10L, 100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(apunteService).eliminarApunte(100L, 1L);
    }

    @Test
    void eliminarApunteShouldReturnNotFoundWhenCommunityDoesNotMatch() {
        ApunteResponse otro = ApunteResponse.builder().id(100L).comunidadId(99L).build();
        when(apunteService.obtenerApunte(100L)).thenReturn(otro);

        ResponseEntity<Void> response = controller.eliminarApunte(10L, 100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eliminarApunteShouldReturnForbiddenWhenServiceThrows() {
        when(apunteService.obtenerApunte(100L)).thenReturn(apunteResponse);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Sin permisos"))
                .when(apunteService)
                .eliminarApunte(100L, 1L);

        ResponseEntity<Void> response = controller.eliminarApunte(10L, 100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
