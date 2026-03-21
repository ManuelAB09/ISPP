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

import es.us.meerkat.backend.dto.AgregarGrabacionRequest;
import es.us.meerkat.backend.dto.GrabacionClaseResponse;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.GrabacionClase;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.GrabacionClaseRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de grabaciones de clase y su sincronización con Google Classroom. */
@Service
@RequiredArgsConstructor
public class GrabacionClaseService {

    private static final Logger LOG = LoggerFactory.getLogger(GrabacionClaseService.class);

    private final GrabacionClaseRepository grabacionClaseRepository;
    private final ComunidadRepository comunidadRepository;
    private final GoogleClassroomService googleClassroomService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Devuelve todas las grabaciones de una comunidad ordenadas por fecha de creación. */
    @Transactional(readOnly = true)
    public List<GrabacionClaseResponse> getGrabaciones(Long comunidadId) {
        return grabacionClaseRepository.findByComunidadIdOrderByCreatedAtDesc(comunidadId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Agrega una grabación a la comunidad y, opcionalmente, la comparte en Classroom. */
    @Transactional
    public GrabacionClaseResponse agregarGrabacion(
            Long comunidadId, AgregarGrabacionRequest req, Usuario subidoPor) {

        var comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Comunidad no encontrada: " + comunidadId));

        GrabacionClase grabacion =
                GrabacionClase.builder()
                        .evento(null)
                        .comunidad(comunidad)
                        .urlGrabacion(req.urlGrabacion())
                        .googleDriveFileId(req.googleDriveFileId())
                        .titulo(req.titulo())
                        .descripcion(req.descripcion())
                        .durationSeconds(req.durationSeconds())
                        .subidoPor(subidoPor)
                        .estado("DISPONIBLE")
                        .build();

        grabacion = grabacionClaseRepository.save(grabacion);

        if (Boolean.TRUE.equals(req.compartirEnClassroom())) {
            Optional<ComunidadClassroom> vinculacion =
                    googleClassroomService.getVinculacion(comunidadId);
            if (vinculacion.isPresent()) {
                grabacion = compartirGrabacionEnClassroom(grabacion, vinculacion.get(), subidoPor);
            }
        }

        return toResponse(grabacion);
    }

    private GrabacionClase compartirGrabacionEnClassroom(
            GrabacionClase grabacion, ComunidadClassroom vinculacion, Usuario usuario) {

        try {
            String accessToken = googleClassroomService.getAccessTokenValido(usuario);
            String courseId = vinculacion.getClassroomCourseId();
            String url =
                    "https://classroom.googleapis.com/v1/courses/"
                            + courseId
                            + "/courseWorkMaterials";

            String descripcionClassroom =
                    "[Grabación] "
                            + (grabacion.getDescripcion() != null
                                    ? grabacion.getDescripcion()
                                    : "");

            String body =
                    String.format(
                            "{"
                                    + "\"title\":\"%s\","
                                    + "\"description\":\"%s\","
                                    + "\"state\":\"PUBLISHED\","
                                    + "\"materials\":[{\"link\":{\"url\":\"%s\",\"title\":\"%s\"}}]"
                                    + "}",
                            escapeJson(grabacion.getTitulo()),
                            escapeJson(descripcionClassroom),
                            escapeJson(grabacion.getUrlGrabacion()),
                            escapeJson(grabacion.getTitulo()));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                if (json.hasNonNull("id")) {
                    grabacion.setClassroomResourceId(json.get("id").asText());
                    grabacion.setSincronizadoClassroom(true);
                    grabacion = grabacionClaseRepository.save(grabacion);
                }
            }
        } catch (Exception e) {
            LOG.error(
                    "Error al compartir grabación en Classroom para grabacion id={}: {}",
                    grabacion.getId(),
                    e.getMessage(),
                    e);
        }

        return grabacion;
    }

    private GrabacionClaseResponse toResponse(GrabacionClase g) {
        return new GrabacionClaseResponse(
                g.getId(),
                g.getTitulo(),
                g.getDescripcion(),
                g.getUrlGrabacion(),
                g.getGoogleDriveFileId(),
                g.getDurationSeconds(),
                g.getSincronizadoClassroom(),
                g.getEstado(),
                g.getSubidoPor() != null ? g.getSubidoPor().getId() : null,
                g.getSubidoPor() != null ? g.getSubidoPor().getNombre() : null,
                g.getCreatedAt());
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
