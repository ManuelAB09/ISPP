package es.us.meerkat.backend.controller.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.forms.CreateCuestionarioRequest;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.forms.CuestionarioService;

@ExtendWith(MockitoExtension.class)
class CuestionarioControllerTest {

    @Mock private CuestionarioService cuestionarioService;

    @InjectMocks private CuestionarioController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Cuestionario buildCuestionario(Long id, Usuario usuario) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        c.setTitulo("Test Cuestionario");
        c.setCreador(usuario);
        c.setActivo(true);
        c.setPublicado(false);
        return c;
    }

    @Test
    void listMineShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listMine(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenCuestionarioDoesNotExist() {
        when(cuestionarioService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCuestionarioShouldReturnCreatedWhenUserIsValid() {
        Usuario usuario = buildUsuario(1L);
        CreateCuestionarioRequest request = new CreateCuestionarioRequest();
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.createFromDto(eq(request), eq(usuario))).thenReturn(cuestionario);

        ResponseEntity<?> response = controller.createCuestionario(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createCuestionarioShouldReturnUnauthorizedWhenUserIsNull() {
        CreateCuestionarioRequest request = new CreateCuestionarioRequest();

        ResponseEntity<?> response = controller.createCuestionario(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listMineShouldReturnUserCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findByCreadorId(1L)).thenReturn(List.of(cuestionario));

        ResponseEntity<?> response = controller.listMine(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listByCommunityShouldReturnCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findByComunidadId(1L)).thenReturn(List.of(cuestionario));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listByCommunity(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listByCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<List<Map<String, Object>>> response = controller.listByCommunity(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listAllPublicShouldReturnPublicCuestionarios() {
        Cuestionario cuestionario = buildCuestionario(1L, buildUsuario(1L));
        cuestionario.setPublicado(true);

        when(cuestionarioService.findAllPublic()).thenReturn(List.of(cuestionario));

        ResponseEntity<List<Map<String, Object>>> response = controller.listAllPublic();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listAssignedToMeShouldReturnAssignedCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, buildUsuario(2L));

        when(cuestionarioService.findAssignedToUser(1L)).thenReturn(List.of(cuestionario));

        ResponseEntity<?> response = controller.listAssignedToMe(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listAssignedToMeShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listAssignedToMe(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getByIdShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findById(1L)).thenReturn(Optional.of(cuestionario));

        ResponseEntity<Map<String, Object>> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenDoesNotExist() {
        when(cuestionarioService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getById(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPreviewShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findById(1L)).thenReturn(Optional.of(cuestionario));
        when(cuestionarioService.findAttemptsByUsuarioAndCuestionario(1L, 1L))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.getPreview(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getPreviewShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getPreview(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getResolverShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findById(1L)).thenReturn(Optional.of(cuestionario));

        ResponseEntity<?> response = controller.getResolver(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getResolverShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getResolver(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publishCuestionarioShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);
        cuestionario.setPublicado(true);

        when(cuestionarioService.updatePublicado(1L, true)).thenReturn(cuestionario);

        ResponseEntity<?> response = controller.publishCuestionario(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void publishCuestionarioShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.publishCuestionario(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
