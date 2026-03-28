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

import es.us.meerkat.backend.controller.communities.InvitacionMiembroController;
import es.us.meerkat.backend.dto.communities.CreateInvitacionRequest;
import es.us.meerkat.backend.dto.communities.InvitacionListResponse;
import es.us.meerkat.backend.dto.communities.InvitacionResponse;
import es.us.meerkat.backend.entity.InvitacionMiembro;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.communities.InvitacionMiembroService;

@ExtendWith(MockitoExtension.class)
class InvitacionMiembroControllerTest {

    @Mock private InvitacionMiembroService invitacionService;

    @InjectMocks private InvitacionMiembroController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void listInvitacionesShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        InvitacionMiembro inv = new InvitacionMiembro();

        when(invitacionService.getInvitacionesByCommunity(eq(1L), eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(inv)));

        ResponseEntity<InvitacionListResponse> response =
                controller.listInvitaciones(10L, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void createInvitacionShouldReturnCreated() {
        Usuario usuario = buildUsuario(1L);
        InvitacionMiembro inv = new InvitacionMiembro();

        when(invitacionService.createInvitacion(eq(1L), eq(10L), any())).thenReturn(inv);

        ResponseEntity<InvitacionResponse> response =
                controller.createInvitacion(
                        10L,
                        new CreateInvitacionRequest("test@test.com", RolComunidad.ALUMNO),
                        usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getInvitacionByCodigoShouldReturnOk() {
        InvitacionMiembro inv = new InvitacionMiembro();

        when(invitacionService.getInvitacionByCodigo("ABC123")).thenReturn(inv);

        ResponseEntity<InvitacionResponse> response = controller.getInvitacionByCodigo("ABC123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
