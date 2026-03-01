package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.TutorService;

@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    @Mock private TutorService tutorService;

    @InjectMocks private TutorController tutorController;

    @Test
    void listarTutoresVerificadosShouldReturnPageWhenServiceSucceeds() {
        Page<TutorProfileResponse> page =
                new PageImpl<>(
                        List.of(TutorProfileResponse.builder().id(1L).verificado(true).build()));
        when(tutorService.obtenerTutoresVerificados(
                        "mates", BigDecimal.TEN, BigDecimal.valueOf(50), 0, 20))
                .thenReturn(page);

        ResponseEntity<?> response =
                tutorController.listarTutoresVerificados(
                        "mates", BigDecimal.TEN, BigDecimal.valueOf(50), 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void listarTutoresVerificadosShouldReturn500WhenServiceFails() {
        when(tutorService.obtenerTutoresVerificados(null, null, null, 0, 20))
                .thenThrow(new RuntimeException("error interno"));

        ResponseEntity<?> response =
                tutorController.listarTutoresVerificados(null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isEqualTo("Error al listar tutores verificados: error interno");
    }

    @Test
    void crearPerfilShouldReturnOkWhenServiceSucceeds() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        TutorProfileRequest request = new TutorProfileRequest();
        TutorProfileResponse created = TutorProfileResponse.builder().id(10L).userId(7L).build();

        when(tutorService.crearPerfil(usuario.getId(), request)).thenReturn(created);

        ResponseEntity<?> response = tutorController.crearPerfil(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(created);
        verify(tutorService).crearPerfil(usuario.getId(), request);
    }

    @Test
    void editarPerfilShouldReturnOkWhenServiceSucceeds() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Long tutorId = 55L;
        TutorProfileRequest request = new TutorProfileRequest();
        TutorProfileResponse updated =
                TutorProfileResponse.builder().id(tutorId).userId(7L).build();

        when(tutorService.editarPerfil(usuario.getId(), tutorId, request)).thenReturn(updated);

        ResponseEntity<?> response = tutorController.editarPerfil(usuario, tutorId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(tutorService).editarPerfil(usuario.getId(), tutorId, request);
    }

    @Test
    void verPerfilPublicoShouldReturnOkWithProfile() {
        Long tutorId = 22L;
        TutorProfileResponse perfil = TutorProfileResponse.builder().id(tutorId).build();
        when(tutorService.obtenerPerfilPublico(tutorId)).thenReturn(perfil);

        ResponseEntity<?> response = tutorController.verPerfilPublico(tutorId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(perfil);
        verify(tutorService).obtenerPerfilPublico(tutorId);
    }

    @Test
    void obtenerMisPerfilesShouldReturnOkWithList() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);

        List<TutorProfileResponse> perfiles =
                List.of(TutorProfileResponse.builder().id(1L).build());
        when(tutorService.obtenerPerfilesPorUsuario(usuario.getId())).thenReturn(perfiles);

        ResponseEntity<?> response = tutorController.obtenerMisPerfiles(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(perfiles);
        verify(tutorService).obtenerPerfilesPorUsuario(usuario.getId());
    }

    @Test
    void solicitarVerificacionShouldReturnOkWhenServiceSucceeds() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Long tutorId = 44L;

        ResponseEntity<?> response = tutorController.solicitarVerificacion(usuario, tutorId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tutorService).solicitarVerificacion(usuario.getId(), tutorId);
    }

    @Test
    void obtenerEstadoVerificacionShouldReturnOkWithStatus() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Long tutorId = 44L;
        when(tutorService.obtenerEstadoVerificacion(usuario.getId(), tutorId))
                .thenReturn("PENDIENTE");

        ResponseEntity<?> response = tutorController.obtenerEstadoVerificacion(usuario, tutorId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("PENDIENTE");
        verify(tutorService).obtenerEstadoVerificacion(usuario.getId(), tutorId);
    }
}
