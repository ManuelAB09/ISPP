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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.communities.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.communities.InstitutionResponse;
import es.us.meerkat.backend.dto.communities.UpdateInstitutionRequest;
import es.us.meerkat.backend.dto.subscriptions.CorporatePlanRequest;
import es.us.meerkat.backend.dto.subscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.entity.communities.Institution;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.InstitutionService;
import es.us.meerkat.backend.service.subscriptions.PaymentService;

@ExtendWith(MockitoExtension.class)
class InstitutionControllerTest {

    @Mock private InstitutionService institutionService;
    @Mock private PaymentService paymentService;

    @InjectMocks private InstitutionController controller;

    private Usuario usuario;
    private Institution institution;

    @BeforeEach
    void setUp() {
        usuario = buildUsuario(1L);
        institution = new Institution();
        institution.setId(10L);
        institution.setNombre("Test University");
        institution.setDominioEmail("test.edu");
    }

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void crearInstitucionShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                controller.crearInstitucion(null, new CreateInstitutionRequest());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearInstitucionShouldReturnCreatedOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNombre("Test University");
        institution.setDominioEmail("test.edu");

        when(institutionService.crearInstitucion(eq(1L), any())).thenReturn(institution);

        ResponseEntity<?> response =
                controller.crearInstitucion(usuario, new CreateInstitutionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearInstitucionShouldReturnConflictOnDomainDuplicate() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.crearInstitucion(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("El dominio ya está registrado"));

        ResponseEntity<?> response =
                controller.crearInstitucion(usuario, new CreateInstitutionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void crearInstitucionShouldReturnBadRequestOnGenericError() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.crearInstitucion(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Datos inválidos"));

        ResponseEntity<?> response =
                controller.crearInstitucion(usuario, new CreateInstitutionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerInstitucionShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNombre("Test University");

        when(institutionService.obtenerInstitucion(10L, 1L)).thenReturn(institution);

        ResponseEntity<InstitutionResponse> response = controller.obtenerInstitucion(10L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void actualizarInstitucionShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNombre("Updated University");

        when(institutionService.actualizarInstitucion(eq(10L), eq(1L), any()))
                .thenReturn(institution);

        ResponseEntity<InstitutionResponse> response =
                controller.actualizarInstitucion(10L, usuario, new UpdateInstitutionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actualizarInstitucionShouldReturnForbiddenWhenUserIsNotAdmin() {
        when(institutionService.actualizarInstitucion(eq(10L), eq(2L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No es admin"));

        Usuario usuario = buildUsuario(2L);
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.actualizarInstitucion(
                                        10L, usuario, new UpdateInstitutionRequest()));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listarInstitucionesShouldReturnOk() {
        when(institutionService.listarInstituciones()).thenReturn(java.util.List.of(institution));

        ResponseEntity<?> response = controller.listarInstituciones();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(institutionService).listarInstituciones();
    }

    @Test
    void contratarPlanCorporativoShouldReturnOkWhenUserIsAdmin() {
        Usuario usuario = buildUsuario(1L);
        PaymentUrlResponse paymentUrl =
                new PaymentUrlResponse("https://stripe.com/pay", "session_123");

        when(institutionService.contratarPlanCorporativo(eq(10L), eq(1L), any()))
                .thenReturn(paymentUrl);

        ResponseEntity<?> response =
                controller.contratarPlanCorporativo(10L, usuario, new CorporatePlanRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void contratarPlanCorporativoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                controller.contratarPlanCorporativo(10L, null, new CorporatePlanRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void contratarPlanCorporativoShouldReturnBadRequestWhenServiceFails() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.contratarPlanCorporativo(eq(10L), eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Plan no válido"));

        ResponseEntity<?> response =
                controller.contratarPlanCorporativo(10L, usuario, new CorporatePlanRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelarPlanCorporativoShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.obtenerInstitucion(10L, 1L)).thenReturn(institution);

        ResponseEntity<InstitutionResponse> response =
                controller.cancelarPlanCorporativo(10L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(institutionService).cancelarPlanCorporativo(10L, 1L);
    }

    @Test
    void crearInstitucionShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.crearInstitucion(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.crearInstitucion(usuario, new CreateInstitutionRequest()));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtenerInstitucionShouldReturnNotFoundWhenNotExists() {
        Usuario usuario = buildUsuario(1L);
        when(institutionService.obtenerInstitucion(999L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.obtenerInstitucion(999L, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
