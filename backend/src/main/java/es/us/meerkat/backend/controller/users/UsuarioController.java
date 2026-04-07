package es.us.meerkat.backend.controller.users;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.chats.MessageResponse;
import es.us.meerkat.backend.dto.events.AttendanceResponse;
import es.us.meerkat.backend.dto.recommendations.FeedbackResponse;
import es.us.meerkat.backend.dto.users.ChangePasswordRequest;
import es.us.meerkat.backend.dto.users.UpdateUserRequest;
import es.us.meerkat.backend.dto.users.UserActivityResponse;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.dto.users.UserSimpleResponse;
import es.us.meerkat.backend.dto.users.VisibilityRequest;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.forms.CuestionarioIntento;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioIntentoRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.users.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de usuarios.
 *
 * <p>Implementa los endpoints del tag Usuarios del OpenAPI. Usa {@link AuthenticationPrincipal}
 * para obtener el usuario autenticado directamente del contexto de seguridad. Base URL:
 * /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public final class UsuarioController {

    /** Servicio para operaciones de usuario. */
    private final UsuarioService usuarioService;

    private final CuestionarioIntentoRepository intentoRepository;
    private final AsistenciaEventoRepository asistenciaEventoRepository;
    private final FeedbackRepository feedbackRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve el perfil completo del usuario autenticado.
     *
     * <p>GET /api/v1/users/me
     *
     * @param usuario Usuario autenticado (del token JWT).
     * @return Perfil completo del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getMe(
            @AuthenticationPrincipal final Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(usuarioService.obtenerPerfilPropio(usuario));
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * <p>PUT /api/v1/users/me
     *
     * @param usuario Usuario autenticado.
     * @param request Datos a actualizar.
     * @return Perfil actualizado.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDetailResponse> updateMe(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody final UpdateUserRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(usuario, request));
    }

    /**
     * Sube una foto de perfil personalizada para el usuario autenticado.
     *
     * <p>POST /api/v1/users/me/photo (multipart/form-data)
     *
     * @param usuario Usuario autenticado.
     * @param file Archivo de imagen a usar como foto de perfil.
     * @return Perfil actualizado.
     */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDetailResponse> uploadProfilePhoto(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestParam("file") final MultipartFile file) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(usuarioService.actualizarFotoPerfil(usuario, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * <p>DELETE /api/v1/users/me Devuelve 204 sin contenido tras la eliminación.
     *
     * @param usuario Usuario autenticado a eliminar.
     * @return Respuesta vacía con código 204.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal final Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        usuarioService.eliminarCuenta(usuario);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>PUT /api/v1/users/me/password
     *
     * @param usuario Usuario autenticado.
     * @param request Contraseña actual y nueva.
     * @return Mensaje de confirmación.
     */
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody final ChangePasswordRequest request) {
        usuarioService.cambiarPassword(usuario, request);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Contraseña actualizada correctamente").build());
    }

    /**
     * Actualiza la visibilidad del perfil en listados públicos.
     *
     * <p>PUT /api/v1/users/me/visibility
     *
     * @param usuario Usuario autenticado.
     * @param request Nueva configuración de visibilidad.
     * @return Perfil actualizado con nueva visibilidad.
     */
    @PutMapping("/me/visibility")
    public ResponseEntity<UserDetailResponse> updateVisibility(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final VisibilityRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarVisibilidad(usuario, request));
    }

    /**
     * Devuelve el perfil público de un usuario por su ID.
     *
     * <p>GET /api/v1/users/{userId} Accesible sin autenticación si el perfil es visible.
     *
     * @param userId ID del usuario cuyo perfil se quiere ver.
     * @return Perfil público del usuario.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicResponse> getUserById(@PathVariable final Long userId) {
        return ResponseEntity.ok(usuarioService.obtenerPerfilPublico(userId));
    }

    /**
     * Devuelve la actividad del usuario autenticado: cuestionarios completados y asistencias. GET
     * /api/v1/users/me/activity
     */
    @GetMapping("/me/activity")
    public ResponseEntity<UserActivityResponse> getMyActivity(
            @AuthenticationPrincipal final Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        UserActivityResponse.UserActivityResponseBuilder builder = UserActivityResponse.builder();

        // Cuestionarios completados — si existen intentos, los devolvemos.
        List<CuestionarioIntento> intentos =
                intentoRepository.findWithCuestionarioByUsuarioId(usuario.getId());
        List<UserActivityResponse.QuizResult> quizResults = new ArrayList<>();
        for (CuestionarioIntento ci : intentos) {
            UserActivityResponse.QuizResult qr =
                    UserActivityResponse.QuizResult.builder()
                            .intentoId(ci.getId())
                            .cuestionarioId(
                                    ci.getCuestionario() != null
                                            ? ci.getCuestionario().getId()
                                            : null)
                            .titulo(
                                    ci.getCuestionario() != null
                                            ? ci.getCuestionario().getTitulo()
                                            : null)
                            .puntuacion(ci.getPuntuacion())
                            .fecha(ci.getCreatedAt())
                            .build();
            quizResults.add(qr);
        }
        builder.cuestionarios(quizResults);

        // Asistencias: si existe repositorio y datos, incluirlos.
        List<AttendanceResponse> attendanceDtos = new ArrayList<>();
        List<AsistenciaEvento> asistencias =
                asistenciaEventoRepository.findConfirmadasByUsuarioId(usuario.getId());
        if (asistencias != null && !asistencias.isEmpty()) {
            for (AsistenciaEvento a : asistencias) {
                attendanceDtos.add(a.toDTO());
            }
        }
        builder.asistencias(attendanceDtos);

        // Feedbacks recibidos (últimos 20)
        List<FeedbackResponse> feedbackDtos = new ArrayList<>();
        var feedbacksPage =
                feedbackRepository.findByAlumnoId(usuario.getId(), PageRequest.of(0, 20));
        feedbacksPage.forEach(f -> feedbackDtos.add(FeedbackResponse.fromEntity(f)));
        builder.feedbacks(feedbackDtos);

        return ResponseEntity.ok(builder.build());
    }

    /**
     * Devuelve los avatares predefinidos disponibles para foto de perfil.
     *
     * <p>GET /api/v1/users/profile-avatars
     *
     * @return Lista de rutas públicas de avatares.
     */
    @GetMapping("/profile-avatars")
    public ResponseEntity<List<String>> getProfileAvatars() {
        return ResponseEntity.ok(usuarioService.obtenerAvataresPerfilDisponibles());
    }

    /**
     * Busca usuarios por nombre o email para asignación en cuestionarios.
     *
     * <p>GET /api/v1/users/search
     *
     * @param search Término de búsqueda (nombre o email)
     * @param usuario Usuario autenticado (para excluirse de los resultados)
     * @return Lista de usuarios que coinciden con la búsqueda
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSimpleResponse>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String search,
            @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        if (search.trim().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<Usuario> results =
                usuarioRepository.searchByNombreOrEmail(search.trim(), usuario.getId());
        List<UserSimpleResponse> dtos =
                results.stream()
                        .map(
                                u ->
                                        new UserSimpleResponse(
                                                u.getId(),
                                                u.getNombre(),
                                                u.getEmail(),
                                                u.getFoto()))
                        .toList();

        return ResponseEntity.ok(dtos);
    }
}
