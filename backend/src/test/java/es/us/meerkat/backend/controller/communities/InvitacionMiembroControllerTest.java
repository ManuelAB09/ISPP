package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import es.us.meerkat.backend.dto.communities.CreateInvitacionRequest;
import es.us.meerkat.backend.dto.communities.InvitacionListResponse;
import es.us.meerkat.backend.dto.communities.InvitacionResponse;
import es.us.meerkat.backend.entity.communities.InvitacionMiembro;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.InvitacionMiembroService;

@ExtendWith(MockitoExtension.class)
class InvitacionMiembroControllerTest {

    @Mock private InvitacionMiembroService invitacionService;

    @InjectMocks private InvitacionMiembroController controller;

    private Usuario usuario;
    private InvitacionMiembro invitacion;
    private CreateInvitacionRequest createRequest;

    @BeforeEach
    void setUp() {
        usuario = buildUsuario(1L);
        invitacion = new InvitacionMiembro();
        invitacion.setId(100L);
        invitacion.setEmail("test@test.com");
        invitacion.setCodigo("ABC123");
        createRequest = new CreateInvitacionRequest("test@test.com", RolComunidad.ALUMNO);
    }

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void listInvitacionesShouldReturnOk() {
        when(invitacionService.getInvitacionesByCommunity(eq(1L), eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(invitacion)));

        ResponseEntity<InvitacionListResponse> response =
                controller.listInvitaciones(10L, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(invitacionService).getInvitacionesByCommunity(eq(1L), eq(10L), any());
    }

    @Test
    void listInvitacionesShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario notAdmin = buildUsuario(2L);
        when(invitacionService.getInvitacionesByCommunity(eq(2L), eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No es admin"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.listInvitaciones(10L, 0, 20, notAdmin));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listInvitacionesShouldReturnOkWithEmptyList() {
        when(invitacionService.getInvitacionesByCommunity(eq(1L), eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<InvitacionListResponse> response =
                controller.listInvitaciones(10L, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().invitaciones()).isEmpty();
    }

    @Test
    void createInvitacionShouldReturnCreated() {
        when(invitacionService.createInvitacion(eq(1L), eq(10L), any())).thenReturn(invitacion);

        ResponseEntity<InvitacionResponse> response =
                controller.createInvitacion(10L, createRequest, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(invitacionService).createInvitacion(eq(1L), eq(10L), any());
    }

    @Test
    void createInvitacionShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario notAdmin = buildUsuario(2L);
        when(invitacionService.createInvitacion(eq(2L), eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No es admin"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.createInvitacion(10L, createRequest, notAdmin));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createInvitacionShouldReturnBadRequestWhenEmailAlreadyExists() {
        when(invitacionService.createInvitacion(eq(1L), eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ya existe"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.createInvitacion(10L, createRequest, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getInvitacionByCodigoShouldReturnOk() {
        when(invitacionService.getInvitacionByCodigo("ABC123")).thenReturn(invitacion);

        ResponseEntity<InvitacionResponse> response = controller.getInvitacionByCodigo("ABC123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invitacionService).getInvitacionByCodigo("ABC123");
    }

    @Test
    void getInvitacionByCodigoShouldReturnNotFoundWhenNotExists() {
        when(invitacionService.getInvitacionByCodigo("INVALID"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.getInvitacionByCodigo("INVALID"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void acceptInvitationShouldReturnOkWhenUserIsNotNull() {
        Usuario newUser = buildUsuario(5L);
        when(invitacionService.aceptarInvitacion("ABC123", newUser.getId()))
                .thenReturn(new InvitacionMiembro());

        ResponseEntity<?> response = controller.aceptarInvitacion("ABC123", newUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invitacionService).aceptarInvitacion("ABC123", newUser.getId());
    }

    @Test
    void rejectInvitationShouldReturnOk() {
        when(invitacionService.rechazarInvitacion("ABC123")).thenReturn(new InvitacionMiembro());

        ResponseEntity<?> response = controller.rechazarInvitacion("ABC123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invitacionService).rechazarInvitacion("ABC123");
    }

    @Test
    void rejectInvitationShouldReturnNotFoundWhenCodeNotFound() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(invitacionService)
                .rechazarInvitacion("INVALID");

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.rechazarInvitacion("INVALID"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void acceptInvitationShouldReturnNotFoundWhenCodeNotFound() {
        Usuario newUser = buildUsuario(5L);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(invitacionService)
                .aceptarInvitacion("INVALID", newUser.getId());

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.aceptarInvitacion("INVALID", newUser));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getInvitacionByCodigoShouldHandleEmptyString() {
        when(invitacionService.getInvitacionByCodigo(""))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class, () -> controller.getInvitacionByCodigo(""));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createInvitacionWithNullEmailShouldThrowException() {
        CreateInvitacionRequest invalidRequest =
                new CreateInvitacionRequest(null, RolComunidad.ALUMNO);
        when(invitacionService.createInvitacion(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Email cannot be null"));

        try {
            controller.createInvitacion(10L, invalidRequest, usuario);
            org.junit.jupiter.api.Assertions.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Email cannot be null");
        }
    }

    @Test
    void createInvitacionWithInvalidEmailShouldThrowException() {
        CreateInvitacionRequest invalidRequest =
                new CreateInvitacionRequest("invalid-email", RolComunidad.ALUMNO);
        when(invitacionService.createInvitacion(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        try {
            controller.createInvitacion(10L, invalidRequest, usuario);
            org.junit.jupiter.api.Assertions.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Invalid email format");
        }
    }

    @Test
    void listInvitacionesShouldReturnOkWithPagination() {
        when(invitacionService.getInvitacionesByCommunity(eq(1L), eq(10L), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(invitacion),
                                org.springframework.data.domain.PageRequest.of(0, 20),
                                1));

        ResponseEntity<InvitacionListResponse> response =
                controller.listInvitaciones(10L, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().invitaciones()).hasSize(1);
    }

    @Test
    void acceptInvitationWithExceptionalStatusShouldPropagateException() {
        Usuario newUser = buildUsuario(5L);
        when(invitacionService.aceptarInvitacion("ABC123", newUser.getId()))
                .thenThrow(new RuntimeException("Service error"));

        RuntimeException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        RuntimeException.class,
                        () -> controller.aceptarInvitacion("ABC123", newUser));

        assertThat(exception.getMessage()).isEqualTo("Service error");
    }

    @Test
    void rejectInvitationShouldPropagateForbiddenException() {
        when(invitacionService.rechazarInvitacion("ABC123"))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "User cannot reject this invitation"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.rechazarInvitacion("ABC123"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
