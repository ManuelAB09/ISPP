package es.us.meerkat.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.dto.ClassroomUserResponse;
import es.us.meerkat.backend.dto.CreateStudentRequest;
import es.us.meerkat.backend.dto.CreateTeacherRequest;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.AuthorizationService;
import es.us.meerkat.backend.service.GoogleClassroomService;

@RestController
@RequestMapping("/oauth2")
public class GoogleClassroomController {

    private final GoogleClassroomService googleClassroomService;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadClassroomRepository comunidadClassroomRepository;
    private final AuthorizationService authorizationService;

    public GoogleClassroomController(
            final GoogleClassroomService googleClassroomService,
            final UsuarioRepository usuarioRepository,
            final ComunidadClassroomRepository comunidadClassroomRepository,
            final AuthorizationService authorizationService) {
        this.googleClassroomService = googleClassroomService;
        this.usuarioRepository = usuarioRepository;
        this.comunidadClassroomRepository = comunidadClassroomRepository;
        this.authorizationService = authorizationService;
    }

    @Value("${google.classroom.client-id}")
    private String clientId;

    @Value("${google.classroom.client-secret}")
    private String clientSecret;

    @Value("${google.classroom.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static record OAuthCtx(Long userId, Long communityId, Long requestId) {}

    private final Map<String, OAuthCtx> oauthStateStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @GetMapping("/authorize/google-classroom-url")
    public ResponseEntity<Map<String, String>> authorizeUrl(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long requestId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        Long userId;

        if (principal instanceof Usuario) {
            userId = ((Usuario) principal).getId();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        String state = generateState();
        oauthStateStore.put(state, new OAuthCtx(userId, communityId, requestId));

        String scope =
                URLEncoder.encode(
                        "https://www.googleapis.com/auth/classroom.courses.readonly",
                        StandardCharsets.UTF_8);

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id="
                        + clientId
                        + "&redirect_uri="
                        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope="
                        + scope
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&state="
                        + URLEncoder.encode(state, StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/authorize/google-classroom")
    public ResponseEntity<Void> authorize() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        Long userId;

        if (principal instanceof Usuario) {
            userId = ((Usuario) principal).getId();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String state = generateState();
        oauthStateStore.put(state, new OAuthCtx(userId, null, null));
        String scope =
                URLEncoder.encode(
                        "https://www.googleapis.com/auth/classroom.courses.readonly",
                        StandardCharsets.UTF_8);
        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id="
                        + clientId
                        + "&redirect_uri="
                        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope="
                        + scope
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&state="
                        + URLEncoder.encode(state, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback/google-classroom")
    public ResponseEntity<String> callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state", required = false) String state)
            throws Exception {

        if (error != null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError(error));
        }

        if (code == null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError("no_code"));
        }

        if (state == null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError("no_state"));
        }

        OAuthCtx ctx = oauthStateStore.remove(state);
        if (ctx == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("invalid_state"));
        }

        Long userId = ctx.userId();
        Long communityId = ctx.communityId(); // puede ser null
        Long requestId = ctx.requestId(); // puede ser null

        Usuario usuarioActual = usuarioRepository.findById(userId).orElse(null);
        if (usuarioActual == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("user_not_found:" + userId));
        }

        // Intercambiar code por tokens
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> tokenResp = rest.postForEntity(tokenUrl, request, String.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful() || tokenResp.getBody() == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("token_error"));
        }

        JsonNode tokenJson = mapper.readTree(tokenResp.getBody());

        String accessToken =
                tokenJson.hasNonNull("access_token")
                        ? tokenJson.get("access_token").asText()
                        : null;

        if (accessToken == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("no_access_token"));
        }

        String refreshToken =
                tokenJson.hasNonNull("refresh_token")
                        ? tokenJson.get("refresh_token").asText()
                        : null;

        long expiresIn =
                tokenJson.hasNonNull("expires_in") ? tokenJson.get("expires_in").asLong() : 3600;

        googleClassroomService.guardarConexionOAuth(
                usuarioActual, accessToken, refreshToken, expiresIn);

        // Listar cursos
        String coursesUrl = "https://classroom.googleapis.com/v1/courses";
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        HttpEntity<Void> coursesReq = new HttpEntity<>(authHeaders);

        ResponseEntity<String> coursesResp =
                rest.exchange(coursesUrl, HttpMethod.GET, coursesReq, String.class);

        String payload = "{}";
        if (coursesResp.getStatusCode().is2xxSuccessful() && coursesResp.getBody() != null) {
            payload = coursesResp.getBody();
        }

        String safe = payload.replace("</", "<\\/");

        String html =
                "<html><body><script>"
                        + "window.opener.postMessage({courses:"
                        + safe
                        + ", communityId:"
                        + (communityId == null ? "null" : communityId)
                        + ", requestId:"
                        + (requestId == null ? "null" : requestId)
                        + "}, '*');"
                        + "window.close();"
                        + "</script></body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private String htmlError(String code) {
        if (code == null) {
            code = "error";
        }
        String safe = code.replace("'", "\\'");
        return "<html><body><script>window.opener.postMessage({error:'"
                + safe
                + "'}, '*');window.close();</script></body></html>";
    }

    // =====================================================
    // ENDPOINTS PARA GESTIÓN DE ESTUDIANTES Y PROFESORES
    // =====================================================

    /**
     * GET /oauth2/authorize/google-classroom-management Genera URL para autorizar acceso a gestión
     * de estudiantes y profesores.
     */
    @GetMapping("/authorize/google-classroom-management")
    public ResponseEntity<Map<String, String>> authorizeManagementUrl() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        Long userId;

        if (principal instanceof Usuario) {
            userId = ((Usuario) principal).getId();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        String state = generateState();
        oauthStateStore.put(state, new OAuthCtx(userId, null, null));

        // Scopes para gestionar estudiantes y profesores
        String[] scopes = {
            "https://www.googleapis.com/auth/classroom.profile.emails",
            "https://www.googleapis.com/auth/classroom.profile.photos",
            "https://www.googleapis.com/auth/classroom.rosters"
        };

        String scopeParam = URLEncoder.encode(String.join(" ", scopes), StandardCharsets.UTF_8);

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id="
                        + clientId
                        + "&redirect_uri="
                        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope="
                        + scopeParam
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&state="
                        + URLEncoder.encode(state, StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * GET /oauth2/callback/google-classroom-management Callback que maneja la autorización y guarda
     * el token.
     */
    @GetMapping("/callback/google-classroom-management")
    public ResponseEntity<String> callbackManagement(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state", required = false) String state)
            throws Exception {
        if (error != null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError(error));
        }
        if (code == null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError("no_code"));
        }

        if (state == null) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlError("no_state"));
        }

        OAuthCtx ctx = oauthStateStore.remove(state);
        if (ctx == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("invalid_state"));
        }

        Long userId = ctx.userId();

        Usuario usuarioActual = usuarioRepository.findById(userId).orElse(null);
        if (usuarioActual == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("user_not_found:" + userId));
        }

        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> tokenResp = rest.postForEntity(tokenUrl, request, String.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("token_error"));
        }

        JsonNode tokenJson = mapper.readTree(tokenResp.getBody());
        String accessToken = tokenJson.get("access_token").asText();
        if (accessToken == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlError("no_access_token"));
        }
        String refreshToken =
                tokenJson.has("refresh_token") ? tokenJson.get("refresh_token").asText() : null;

        long expiresIn = tokenJson.get("expires_in").asLong();

        googleClassroomService.guardarConexionOAuth(
                usuarioActual, accessToken, refreshToken, expiresIn);

        String safe = "success";
        String html =
                "<html><body><script>window.opener.postMessage({status:'"
                        + safe
                        + "'}, '*');window.close();</script></body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /**
     * POST /oauth2/students/{courseId} Crea un estudiante en un curso de Google Classroom.
     *
     * @param courseId ID del curso en Google Classroom
     * @param request Datos del estudiante (userId, enrollmentCode)
     * @return Datos del estudiante creado
     */
    @PostMapping("/students/{courseId}")
    public ResponseEntity<?> createStudent(
            @PathVariable String courseId, @RequestBody CreateStudentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        Usuario usuario = (Usuario) principal;

        // Verificar que el curso esté vinculado a una comunidad y que el usuario sea ADMIN/PROFESOR
        ComunidadClassroom cc =
                comunidadClassroomRepository.findByClassroomCourseId(courseId).orElse(null);
        if (cc == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Curso no vinculado a ninguna comunidad"));
        }

        Long comunidadId = cc.getComunidad().getId();
        if (!authorizationService.isAdminOrProfesor(usuario.getId(), comunidadId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No autorizado"));
        }

        try {
            // Construir JSON para Google Classroom
            String studentData;
            if (request.enrollmentCode() != null && !request.enrollmentCode().isEmpty()) {
                studentData =
                        String.format(
                                "{\"userId\":\"%s\",\"enrollmentCode\":\"%s\"}",
                                request.userId(), request.enrollmentCode());
            } else {
                studentData = String.format("{\"userId\":\"%s\"}", request.userId());
            }

            ClassroomUserResponse response =
                    googleClassroomService.crearEstudiante(usuario, courseId, studentData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /oauth2/communities/{communityId}/files Devuelve los archivos/materiales del curso
     * vinculado a la comunidad para el usuario autenticado.
     */
    @GetMapping("/communities/{communityId}/files")
    public ResponseEntity<?> listarArchivosCursoVinculado(@PathVariable Long communityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        Usuario usuario = (Usuario) principal;

        if (!authorizationService.isAdminOrProfesor(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No autorizado"));
        }

        try {
            Map<String, Object> resp =
                    googleClassroomService.listarArchivosCursoVinculado(usuario, communityId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /oauth2/teachers/{courseId} Crea un profesor en un curso de Google Classroom.
     *
     * @param courseId ID del curso en Google Classroom
     * @param request Datos del profesor (userId)
     * @return Datos del profesor creado
     */
    @PostMapping("/teachers/{courseId}")
    public ResponseEntity<?> createTeacher(
            @PathVariable String courseId, @RequestBody CreateTeacherRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        Usuario usuario = (Usuario) principal;

        ComunidadClassroom cc =
                comunidadClassroomRepository.findByClassroomCourseId(courseId).orElse(null);
        if (cc == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Curso no vinculado a ninguna comunidad"));
        }

        Long comunidadId = cc.getComunidad().getId();
        if (!authorizationService.isAdminOrProfesor(usuario.getId(), comunidadId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No autorizado"));
        }

        try {
            String teacherData = String.format("{\"userId\":\"%s\"}", request.userId());

            ClassroomUserResponse response =
                    googleClassroomService.crearProfesor(usuario, courseId, teacherData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /oauth2/communities/{communityId}/students/stats Devuelve estadísticas básicas de los
     * alumnos del curso vinculado a la comunidad.
     */
    @GetMapping("/communities/{communityId}/students/stats")
    public ResponseEntity<?> obtenerEstadisticasAlumnos(@PathVariable Long communityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_user"));
        }

        Usuario usuario = (Usuario) principal;

        if (!authorizationService.isAdminOrProfesor(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No autorizado"));
        }

        try {
            Map<String, Object> stats =
                    googleClassroomService.obtenerEstadisticasAlumnos(usuario, communityId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
