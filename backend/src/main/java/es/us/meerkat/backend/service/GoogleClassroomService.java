package es.us.meerkat.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.dto.ClassroomUserResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.GoogleClassroomConnection;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.GoogleClassroomConnectionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleClassroomService {

    private final GoogleClassroomConnectionRepository connectionRepository;
    private final ComunidadClassroomRepository comunidadClassroomRepository;
    private final ComunidadRepository comunidadRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UsuarioRepository usuarioRepository;

    @Value("${google.classroom.client-id}")
    private String clientId;

    @Value("${google.classroom.client-secret}")
    private String clientSecret;

    @Value("${google.classroom.redirect-uri}")
    private String redirectUri;

    public static record OAuthCtx(Long userId, Long communityId, Long requestId) {}

    public final Map<String, OAuthCtx> oauthStateStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Genera una URL de autorización de Google OAuth para Classroom y guarda el estado para
     * asociarlo al usuario cuando Google llame al callback.
     *
     * @param userId id del usuario local
     * @param communityId id de comunidad opcional
     * @param requestId id de request opcional
     * @param management si true, genera la URL para scopes de gestión
     * @return URL completa de autorización
     */
    public String buildAuthorizeUrlForUser(
            Long userId, Long communityId, Long requestId, boolean management) {
        usuarioRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String state = generateState();
        oauthStateStore.put(state, new OAuthCtx(userId, communityId, requestId));

        String scope;
        String url;
        if (management) {
            String[] scopes = {
                "https://www.googleapis.com/auth/classroom.profile.emails",
                "https://www.googleapis.com/auth/classroom.profile.photos",
                "https://www.googleapis.com/auth/classroom.rosters"
            };
            scope = URLEncoder.encode(String.join(" ", scopes), StandardCharsets.UTF_8);

            url =
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

        } else {
            String[] commonScopes = {
                "https://www.googleapis.com/auth/classroom.courses.readonly",
                "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
                "https://www.googleapis.com/auth/classroom.coursework.students.readonly",
                "https://www.googleapis.com/auth/classroom.courseworkmaterials.readonly"
            };
            scope = URLEncoder.encode(String.join(" ", commonScopes), StandardCharsets.UTF_8);

            url =
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
        }

        return url;
    }

    public OAuthCtx consumeState(String state) {
        return oauthStateStore.remove(state);
    }

    /** Guarda o actualiza la conexión OAuth del usuario. */
    public void guardarConexionOAuth(
            Usuario usuario, String accessToken, String refreshToken, long expiresInSeconds) {

        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

        GoogleClassroomConnection connection =
                connectionRepository
                        .findByUsuario(usuario)
                        .orElse(GoogleClassroomConnection.builder().usuario(usuario).build());

        connection.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            connection.setRefreshToken(refreshToken);
        }
        connection.setExpiresAt(expiresAt);
        connection.setActiva(true);

        connectionRepository.save(connection);
    }

    /** Devuelve el access token válido del usuario. */
    public String getAccessTokenValido(Usuario usuario) {
        GoogleClassroomConnection connection =
                connectionRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Conexión de Google Classroom no encontrada"));

        if (connection.estaExpirado()) {
            refrescarAccessToken(connection);
        }
        return connection.getAccessToken();
    }

    /** Refresca el access token usando el refresh token. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void refrescarAccessToken(GoogleClassroomConnection connection) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", connection.getRefreshToken());
        form.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> tokenResp = restTemplate.postForEntity(tokenUrl, request, Map.class);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            connection.desactivar();
            connectionRepository.save(connection);
            throw new RuntimeException("Error al refrescar el token de Google Classroom");
        }

        Map<String, Object> body = tokenResp.getBody();
        String newAccessToken = (String) body.get("access_token");
        long expiresIn = ((Integer) body.get("expires_in")).longValue();

        connection.setAccessToken(newAccessToken);
        connection.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        connectionRepository.save(connection);
    }

    // =====================================================
    // VINCULACIÓN COMUNIDAD ↔ GOOGLE CLASSROOM
    // =====================================================

    /** Vincula un curso de Google Classroom a una comunidad. */
    public ComunidadClassroom vincularCurso(Long comunidadId, String courseId, String courseName) {
        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        // Si ya existe una vinculación, la actualizamos
        ComunidadClassroom cc =
                comunidadClassroomRepository
                        .findByComunidad(comunidad)
                        .orElse(ComunidadClassroom.builder().comunidad(comunidad).build());

        cc.setClassroomCourseId(courseId);
        cc.setClassroomCourseName(courseName);
        cc.setActiva(true);

        return comunidadClassroomRepository.save(cc);
    }

    /** Desvincula el curso de Google Classroom de una comunidad. */
    @Transactional
    public void desvincularCurso(Long comunidadId) {
        ComunidadClassroom cc =
                comunidadClassroomRepository
                        .findByComunidadId(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No hay curso vinculado a esta comunidad"));
        comunidadClassroomRepository.delete(cc);
    }

    /** Obtiene la vinculación de una comunidad con Google Classroom, si existe. */
    public Optional<ComunidadClassroom> getVinculacion(Long comunidadId) {
        return comunidadClassroomRepository.findByComunidadId(comunidadId);
    }

    /**
     * Lista archivos/materiales del curso vinculado a la comunidad.
     *
     * @param usuario Usuario que realizará la petición (debe tener conexión OAuth activa)
     * @param comunidadId ID de la comunidad
     * @return Map con claves "materials" y "courseWork" con la información obtenida de Classroom
     */
    public Map<String, Object> listarArchivosCursoVinculado(Usuario usuario, Long comunidadId) {
        ComunidadClassroom vinculacion =
                comunidadClassroomRepository
                        .findByComunidadId(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No hay curso vinculado a la comunidad"));

        String courseId = vinculacion.getClassroomCourseId();

        String accessToken = getAccessTokenValido(usuario);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        String materialsUrl =
                "https://classroom.googleapis.com/v1/courses/" + courseId + "/courseWorkMaterials";
        String courseworkUrl =
                "https://classroom.googleapis.com/v1/courses/" + courseId + "/courseWork";

        ResponseEntity<String> matResp =
                restTemplate.exchange(materialsUrl, HttpMethod.GET, req, String.class);
        ResponseEntity<String> workResp =
                restTemplate.exchange(courseworkUrl, HttpMethod.GET, req, String.class);

        try {
            Map<String, Object> result = new java.util.HashMap<>();

            if (matResp.getStatusCode().is2xxSuccessful() && matResp.getBody() != null) {
                result.put("materials", objectMapper.readValue(matResp.getBody(), Map.class));
            } else {
                result.put("materials", Map.of());
            }

            if (workResp.getStatusCode().is2xxSuccessful() && workResp.getBody() != null) {
                result.put("courseWork", objectMapper.readValue(workResp.getBody(), Map.class));
            } else {
                result.put("courseWork", Map.of());
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Classroom", e);
        }
    }

    /**
     * Obtiene estadísticas básicas de los alumnos del curso vinculado a la comunidad. Retorna un
     * Map con claves como "studentCount" y "students" (lista cruda devuelta por Classroom).
     */
    @SuppressWarnings("rawtypes")
    public Map<String, Object> obtenerEstadisticasAlumnos(Usuario usuario, Long comunidadId) {
        ComunidadClassroom vinculacion =
                comunidadClassroomRepository
                        .findByComunidadId(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No hay curso vinculado a la comunidad"));

        String courseId = vinculacion.getClassroomCourseId();

        String accessToken = getAccessTokenValido(usuario);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        String studentsUrl =
                "https://classroom.googleapis.com/v1/courses/" + courseId + "/students";

        ResponseEntity<String> studentsResp =
                restTemplate.exchange(studentsUrl, HttpMethod.GET, req, String.class);

        try {
            Map<String, Object> result = new java.util.HashMap<>();

            if (studentsResp.getStatusCode().is2xxSuccessful() && studentsResp.getBody() != null) {
                Map parsed = objectMapper.readValue(studentsResp.getBody(), Map.class);
                java.util.List students =
                        parsed.containsKey("students")
                                ? (java.util.List) parsed.get("students")
                                : java.util.List.of();
                result.put("studentCount", students.size());
                result.put("students", students);
            } else {
                result.put("studentCount", 0);
                result.put("students", java.util.List.of());
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Classroom", e);
        }
    }

    // =====================================================
    // GESTIÓN DE ESTUDIANTES Y PROFESORES
    // =====================================================

    /**
     * Crea un estudiante en un curso de Google Classroom.
     *
     * @param usuario Usuario que realiza la acción (debe tener OAuth autorizado)
     * @param courseId ID del curso en Google Classroom
     * @param studentData Datos del estudiante (JSON con userId, enrollmentCode, etc.)
     * @return Respuesta de Google Classroom con los datos del estudiante creado
     */
    public ClassroomUserResponse crearEstudiante(
            Usuario usuario, String courseId, String studentData) {
        String accessToken = getAccessTokenValido(usuario);

        String studentUrl = "https://classroom.googleapis.com/v1/courses/" + courseId + "/students";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(studentData, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(studentUrl, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al crear estudiante: "
                            + response.getStatusCode()
                            + " - "
                            + response.getBody());
        }

        try {
            return objectMapper.readValue(response.getBody(), ClassroomUserResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Google Classroom", e);
        }
    }

    /**
     * Crea un profesor en un curso de Google Classroom.
     *
     * @param usuario Usuario que realiza la acción (debe tener OAuth autorizado)
     * @param courseId ID del curso en Google Classroom
     * @param teacherData Datos del profesor (JSON con userId, etc.)
     * @return Respuesta de Google Classroom con los datos del profesor creado
     */
    public ClassroomUserResponse crearProfesor(
            Usuario usuario, String courseId, String teacherData) {
        String accessToken = getAccessTokenValido(usuario);

        String teacherUrl = "https://classroom.googleapis.com/v1/courses/" + courseId + "/teachers";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(teacherData, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(teacherUrl, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al crear profesor: "
                            + response.getStatusCode()
                            + " - "
                            + response.getBody());
        }

        try {
            return objectMapper.readValue(response.getBody(), ClassroomUserResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Google Classroom", e);
        }
    }

    /**
     * Elimina un estudiante de un curso de Google Classroom.
     *
     * @param usuario Usuario que realiza la acción (debe tener OAuth autorizado)
     * @param courseId ID del curso en Google Classroom
     * @param userId Email o ID del usuario a eliminar
     */
    public void eliminarEstudiante(Usuario usuario, String courseId, String userId) {
        String accessToken = getAccessTokenValido(usuario);

        String studentUrl =
                "https://classroom.googleapis.com/v1/courses/"
                        + courseId
                        + "/students/"
                        + URLEncoder.encode(userId, java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(studentUrl, HttpMethod.DELETE, request, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error al eliminar estudiante: " + response.getStatusCode());
        }
    }

    /**
     * Elimina un profesor de un curso de Google Classroom.
     *
     * @param usuario Usuario que realiza la acción (debe tener OAuth autorizado)
     * @param courseId ID del curso en Google Classroom
     * @param userId Email o ID del usuario a eliminar
     */
    public void eliminarProfesor(Usuario usuario, String courseId, String userId) {
        String accessToken = getAccessTokenValido(usuario);

        String teacherUrl =
                "https://classroom.googleapis.com/v1/courses/"
                        + courseId
                        + "/teachers/"
                        + URLEncoder.encode(userId, java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(teacherUrl, HttpMethod.DELETE, request, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error al eliminar profesor: " + response.getStatusCode());
        }
    }
}
