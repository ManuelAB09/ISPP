package es.us.meerkat.backend.controller.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import es.us.meerkat.backend.dto.forms.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.CuestionarioIntento;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.forms.CuestionarioService;

@ExtendWith(MockitoExtension.class)
class CuestionarioControllerTest {

    @Mock private CuestionarioService cuestionarioService;

    @InjectMocks private CuestionarioController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("user@test.es");
        u.setNombre("Test User");
        return u;
    }

    private Cuestionario buildCuestionario(Long id, Usuario usuario) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        c.setTitulo("Test Cuestionario");
        c.setDescripcion("Test Description");
        c.setCreador(usuario);
        c.setActivo(true);
        c.setPublicado(false);
        return c;
    }

    private CuestionarioIntento buildIntento(Long id, Cuestionario cuestionario, Usuario usuario) {
        CuestionarioIntento intento = new CuestionarioIntento();
        intento.setId(id);
        intento.setCuestionario(cuestionario);
        intento.setUsuario(usuario);
        intento.setPuntuacion(85.0);
        return intento;
    }

    // ============================================================================
    // POST /api/v1/cuestionarios
    // ============================================================================

    @Test
    void createCuestionarioShouldReturnCreatedWhenUserIsValid() {
        Usuario usuario = buildUsuario(1L);
        CreateCuestionarioRequest request = new CreateCuestionarioRequest();
        request.setTitulo("New Quiz");
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.createFromDto(eq(request), eq(usuario))).thenReturn(cuestionario);

        ResponseEntity<?> response = controller.createCuestionario(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(cuestionarioService).createFromDto(request, usuario);
    }

    @Test
    void createCuestionarioShouldReturnUnauthorizedWhenUserIsNull() {
        CreateCuestionarioRequest request = new CreateCuestionarioRequest();

        ResponseEntity<?> response = controller.createCuestionario(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios (mine)
    // ============================================================================

    @Test
    void listMineShouldReturnUserCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findByCreadorId(1L)).thenReturn(List.of(cuestionario));

        ResponseEntity<?> response = controller.listMine(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(cuestionarioService).findByCreadorId(1L);
    }

    @Test
    void listMineShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listMine(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listMineShouldReturnEmptyListWhenNoQuizzes() {
        Usuario usuario = buildUsuario(1L);

        when(cuestionarioService.findByCreadorId(1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.listMine(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/assigned-to-me
    // ============================================================================

    @Test
    void listAssignedToMeShouldReturnOkWithAssignedCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, buildUsuario(2L));

        when(cuestionarioService.findAssignedToUser(1L)).thenReturn(List.of(cuestionario));

        ResponseEntity<?> response = controller.listAssignedToMe(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cuestionarioService).findAssignedToUser(1L);
    }

    @Test
    void listAssignedToMeShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listAssignedToMe(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/public
    // ============================================================================

    @Test
    void listAllPublicShouldReturnPublicCuestionarios() {
        Cuestionario cuestionario = buildCuestionario(1L, buildUsuario(1L));
        cuestionario.setPublicado(true);

        when(cuestionarioService.findAllPublic()).thenReturn(List.of(cuestionario));

        ResponseEntity<List<Map<String, Object>>> response = controller.listAllPublic();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void listAllPublicShouldReturnEmptyListWhenNonePublished() {
        when(cuestionarioService.findAllPublic()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.listAllPublic();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/communities/{communityId}
    // ============================================================================

    @Test
    void listByCommunityShouldReturnCuestionarios() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findByComunidadId(5L)).thenReturn(List.of(cuestionario));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listByCommunity(5L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cuestionarioService).findByComunidadId(5L);
    }

    @Test
    void listByCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<List<Map<String, Object>>> response = controller.listByCommunity(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/{id}
    // ============================================================================

    @Test
    void getByIdShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);

        when(cuestionarioService.findById(1L)).thenReturn(Optional.of(cuestionario));

        ResponseEntity<Map<String, Object>> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cuestionarioService).findById(1L);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenDoesNotExist() {
        when(cuestionarioService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getById(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/{id}/preview
    // ============================================================================

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
    void getPreviewShouldReturnNotFoundWhenQuizNotFound() {
        Usuario usuario = buildUsuario(1L);
        when(cuestionarioService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getPreview(999L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============================================================================
    // GET /api/v1/cuestionarios/{id}/resolver
    // ============================================================================

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
    void getResolverShouldReturnNotFoundWhenQuizNotFound() {
        Usuario usuario = buildUsuario(1L);
        when(cuestionarioService.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getResolver(999L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============================================================================
    // POST /api/v1/cuestionarios/{id}/submit
    // ============================================================================

    @Test
    void submitAttemptShouldReturnOkWhenSuccessful() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, buildUsuario(2L));
        CuestionarioIntento intento = buildIntento(10L, cuestionario, usuario);

        SubmitAttemptRequest request = new SubmitAttemptRequest();

        CuestionarioService.AttemptSubmissionResult resultado =
                new CuestionarioService.AttemptSubmissionResult(intento, 5, 4, List.of());

        when(cuestionarioService.submitAttempt(eq(1L), eq(request), eq(usuario)))
                .thenReturn(resultado);

        ResponseEntity<?> response = controller.submitAttempt(1L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cuestionarioService).submitAttempt(eq(1L), eq(request), eq(usuario));
    }

    @Test
    void submitAttemptShouldReturnUnauthorizedWhenUserIsNull() {
        SubmitAttemptRequest request = new SubmitAttemptRequest();

        ResponseEntity<?> response = controller.submitAttempt(1L, request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void submitAttemptShouldReturnNotFoundOnValidationError() {
        Usuario usuario = buildUsuario(1L);
        SubmitAttemptRequest request = new SubmitAttemptRequest();

        when(cuestionarioService.submitAttempt(eq(1L), eq(request), eq(usuario)))
                .thenThrow(new IllegalArgumentException("Invalid attempt"));

        ResponseEntity<?> response = controller.submitAttempt(1L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============================================================================
    // PUT /api/v1/cuestionarios/{id}/publish
    // ============================================================================

    @Test
    void publishCuestionarioShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        Cuestionario cuestionario = buildCuestionario(1L, usuario);
        cuestionario.setPublicado(true);

        when(cuestionarioService.updatePublicado(1L, true)).thenReturn(cuestionario);

        ResponseEntity<?> response = controller.publishCuestionario(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cuestionarioService).updatePublicado(1L, true);
    }

    @Test
    void publishCuestionarioShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.publishCuestionario(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
