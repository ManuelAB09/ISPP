package es.us.meerkat.backend.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.dto.CompartirRecursoClassroomRequest;
import es.us.meerkat.backend.dto.RecursoClassroomResponse;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.RecursoClassroom;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.RecursoClassroomRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de recursos educativos de Google Classroom. */
@Service
@RequiredArgsConstructor
public class RecursoClassroomService {

    private static final Logger LOG = LoggerFactory.getLogger(RecursoClassroomService.class);

    private final RecursoClassroomRepository recursoClassroomRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadClassroomRepository comunidadClassroomRepository;
    private final GoogleClassroomService googleClassroomService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Devuelve los recursos de una comunidad. Si soloVisibles, filtra por visible=true. */
    @Transactional(readOnly = true)
    public List<RecursoClassroomResponse> getRecursos(Long comunidadId, boolean soloVisibles) {
        List<RecursoClassroom> recursos =
                soloVisibles
                        ? recursoClassroomRepository.findByComunidadIdAndVisibleTrueOrderByOrdenAsc(
                                comunidadId)
                        : recursoClassroomRepository.findByComunidadIdOrderByOrdenAsc(comunidadId);

        return recursos.stream().map(this::toResponse).toList();
    }

    /** Agrega un recurso a la comunidad y, opcionalmente, lo publica en Classroom. */
    @Transactional
    public RecursoClassroomResponse agregarRecurso(
            Long comunidadId, CompartirRecursoClassroomRequest req, Usuario subidoPor) {

        var comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Comunidad no encontrada: " + comunidadId));

        int siguienteOrden =
                recursoClassroomRepository.findByComunidadIdOrderByOrdenAsc(comunidadId).size() + 1;

        RecursoClassroom recurso =
                RecursoClassroom.builder()
                        .comunidad(comunidad)
                        .tipo(req.getTipo())
                        .titulo(req.getTitulo())
                        .descripcion(req.getDescripcion())
                        .url(req.getUrl())
                        .googleDriveFileId(req.getGoogleDriveFileId())
                        .subidoPor(subidoPor)
                        .visible(true)
                        .orden(siguienteOrden)
                        .build();

        recurso = recursoClassroomRepository.save(recurso);

        if (Boolean.TRUE.equals(req.getPublicarEnClassroom())) {
            Optional<ComunidadClassroom> vinculacion =
                    googleClassroomService.getVinculacion(comunidadId);
            if (vinculacion.isPresent()) {
                recurso = publicarRecursoEnClassroom(recurso, vinculacion.get(), subidoPor);
            }
        }

        return toResponse(recurso);
    }

    private RecursoClassroom publicarRecursoEnClassroom(
            RecursoClassroom recurso, ComunidadClassroom vinculacion, Usuario usuario) {

        try {
            String accessToken = googleClassroomService.getAccessTokenValido(usuario);
            String courseId = vinculacion.getClassroomCourseId();
            String url =
                    "https://classroom.googleapis.com/v1/courses/"
                            + courseId
                            + "/courseWorkMaterials";

            String body =
                    String.format(
                            "{"
                                    + "\"title\":\"%s\","
                                    + "\"description\":\"%s\","
                                    + "\"state\":\"PUBLISHED\","
                                    + "\"materials\":[{\"link\":{\"url\":\"%s\",\"title\":\"%s\"}}]"
                                    + "}",
                            escapeJson(recurso.getTitulo()),
                            escapeJson(
                                    recurso.getDescripcion() != null
                                            ? recurso.getDescripcion()
                                            : ""),
                            escapeJson(recurso.getUrl()),
                            escapeJson(recurso.getTitulo()));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                if (json.hasNonNull("id")) {
                    recurso.setClassroomResourceId(json.get("id").asText());
                    recurso.setSincronizadoClassroom(true);
                    recurso = recursoClassroomRepository.save(recurso);
                }
            }
        } catch (Exception e) {
            LOG.error(
                    "Error al publicar recurso en Classroom para recurso id={}: {}",
                    recurso.getId(),
                    e.getMessage(),
                    e);
        }

        return recurso;
    }

    /** Elimina un recurso. */
    @Transactional
    public void eliminarRecurso(Long recursoId, Usuario usuario) {
        RecursoClassroom recurso =
                recursoClassroomRepository
                        .findById(recursoId)
                        .orElseThrow(
                                () -> new RuntimeException("Recurso no encontrado: " + recursoId));
        recursoClassroomRepository.delete(recurso);
    }

    private RecursoClassroomResponse toResponse(RecursoClassroom r) {
        return new RecursoClassroomResponse(
                r.getId(),
                r.getTipo(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getUrl(),
                r.getGoogleDriveFileId(),
                r.getSincronizadoClassroom(),
                r.getVisible(),
                r.getOrden(),
                r.getSubidoPor() != null ? r.getSubidoPor().getId() : null,
                r.getSubidoPor() != null ? r.getSubidoPor().getNombre() : null,
                r.getCreatedAt());
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
