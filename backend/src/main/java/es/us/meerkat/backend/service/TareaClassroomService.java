package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.dto.CalificarEntregaRequest;
import es.us.meerkat.backend.dto.CrearTareaClassroomRequest;
import es.us.meerkat.backend.dto.EntregaTareaResponse;
import es.us.meerkat.backend.dto.EntregarTareaRequest;
import es.us.meerkat.backend.dto.TareaClassroomResponse;
import es.us.meerkat.backend.entity.CalificacionClassroom;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.EntregaTarea;
import es.us.meerkat.backend.entity.TareaClassroom;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.CalificacionClassroomRepository;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EntregaTareaRepository;
import es.us.meerkat.backend.repository.TareaClassroomRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de tareas de Google Classroom. */
@Service
@RequiredArgsConstructor
public class TareaClassroomService {

    private static final Logger LOG = LoggerFactory.getLogger(TareaClassroomService.class);

    private final TareaClassroomRepository tareaClassroomRepository;
    private final EntregaTareaRepository entregaTareaRepository;
    private final CalificacionClassroomRepository calificacionClassroomRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadClassroomRepository comunidadClassroomRepository;
    private final GoogleClassroomService googleClassroomService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Devuelve todas las tareas de una comunidad ordenadas por fecha de vencimiento. */
    @Transactional(readOnly = true)
    public List<TareaClassroomResponse> getTareas(Long comunidadId) {
        return tareaClassroomRepository
                .findByComunidadIdOrderByFechaVencimientoAsc(comunidadId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Crea una tarea en la plataforma y, opcionalmente, la publica en Google Classroom. */
    @Transactional
    public TareaClassroomResponse crearTarea(
            Long comunidadId, CrearTareaClassroomRequest req, Usuario creador) {

        var comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Comunidad no encontrada: " + comunidadId));

        TareaClassroom tarea =
                TareaClassroom.builder()
                        .comunidad(comunidad)
                        .titulo(req.getTitulo())
                        .descripcion(req.getDescripcion())
                        .fechaVencimiento(req.getFechaVencimiento())
                        .puntosMaximos(
                                req.getPuntosMaximos() != null ? req.getPuntosMaximos() : 100)
                        .publicada(Boolean.TRUE.equals(req.getPublicarEnClassroom()))
                        .creadaPor(creador)
                        .estado("ABIERTA")
                        .build();

        tarea = tareaClassroomRepository.save(tarea);

        if (Boolean.TRUE.equals(req.getPublicarEnClassroom())) {
            Optional<ComunidadClassroom> vinculacion =
                    googleClassroomService.getVinculacion(comunidadId);
            if (vinculacion.isPresent()) {
                tarea = publicarTareaEnClassroom(tarea, vinculacion.get(), creador);
            }
        }

        return toResponse(tarea);
    }

    private TareaClassroom publicarTareaEnClassroom(
            TareaClassroom tarea, ComunidadClassroom vinculacion, Usuario usuario) {

        try {
            String accessToken = googleClassroomService.getAccessTokenValido(usuario);
            String courseId = vinculacion.getClassroomCourseId();
            String url = "https://classroom.googleapis.com/v1/courses/" + courseId + "/courseWork";

            LocalDateTime due = tarea.getFechaVencimiento();
            String body =
                    String.format(
                            "{"
                                    + "\"title\":\"%s\","
                                    + "\"description\":\"%s\","
                                    + "\"workType\":\"ASSIGNMENT\","
                                    + "\"state\":\"PUBLISHED\","
                                    + "\"maxPoints\":%d,"
                                    + "\"dueDate\":{\"year\":%d,\"month\":%d,\"day\":%d},"
                                    + "\"dueTime\":{\"hours\":23,\"minutes\":59,\"seconds\":0}"
                                    + "}",
                            escapeJson(tarea.getTitulo()),
                            escapeJson(
                                    tarea.getDescripcion() != null ? tarea.getDescripcion() : ""),
                            tarea.getPuntosMaximos() != null ? tarea.getPuntosMaximos() : 100,
                            due.getYear(),
                            due.getMonthValue(),
                            due.getDayOfMonth());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                if (json.hasNonNull("id")) {
                    tarea.setClassroomCourseWorkId(json.get("id").asText());
                    tarea = tareaClassroomRepository.save(tarea);
                }
            }
        } catch (Exception e) {
            LOG.error(
                    "Error al publicar tarea en Classroom para tarea id={}: {}",
                    tarea.getId(),
                    e.getMessage(),
                    e);
        }

        return tarea;
    }

    /** Registra o actualiza la entrega de un alumno para una tarea. */
    @Transactional
    public EntregaTareaResponse entregarTarea(
            Long tareaId, EntregarTareaRequest req, Usuario alumno) {

        TareaClassroom tarea =
                tareaClassroomRepository
                        .findById(tareaId)
                        .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + tareaId));

        if (!"ABIERTA".equals(tarea.getEstado())) {
            throw new RuntimeException("La tarea no está en estado ABIERTA");
        }

        EntregaTarea entrega =
                entregaTareaRepository
                        .findByTareaIdAndAlumnoId(tareaId, alumno.getId())
                        .orElse(EntregaTarea.builder().tarea(tarea).alumno(alumno).build());

        entrega.setEstado("ENTREGADA");
        entrega.setUrlArchivo(req.urlArchivo());
        entrega.setContenido(req.contenido());
        entrega.setCreatedAt(LocalDateTime.now());

        entrega = entregaTareaRepository.save(entrega);
        return toEntregaResponse(entrega);
    }

    /** Devuelve todas las entregas de una tarea. */
    @Transactional(readOnly = true)
    public List<EntregaTareaResponse> getEntregas(Long tareaId) {
        return entregaTareaRepository.findByTareaIdOrderByCreatedAtDesc(tareaId).stream()
                .map(this::toEntregaResponse)
                .toList();
    }

    /** Califica una entrega y sincroniza la calificación con Classroom si hay vinculación. */
    @Transactional
    public EntregaTareaResponse calificarEntrega(
            Long entregaId, CalificarEntregaRequest req, Usuario profesor) {

        EntregaTarea entrega =
                entregaTareaRepository
                        .findById(entregaId)
                        .orElseThrow(
                                () -> new RuntimeException("Entrega no encontrada: " + entregaId));

        entrega.setCalificacion(req.calificacion());
        entrega.setComentarioRetroalimentacion(req.comentario());
        entrega.setEstado("CALIFICADA");
        entrega.setFechaCalificacion(LocalDateTime.now());
        entrega = entregaTareaRepository.save(entrega);

        TareaClassroom tarea = entrega.getTarea();
        Usuario alumno = entrega.getAlumno();

        CalificacionClassroom calificacion =
                calificacionClassroomRepository
                        .findByTareaIdAndAlumnoId(tarea.getId(), alumno.getId())
                        .orElse(
                                CalificacionClassroom.builder()
                                        .tarea(tarea)
                                        .alumno(alumno)
                                        .build());

        calificacion.setCalificacionPlataforma(req.calificacion());
        calificacion.setFuenteMasReciente("PLATAFORMA");
        calificacionClassroomRepository.save(calificacion);

        if (tarea.getClassroomCourseWorkId() != null) {
            try {
                sincronizarCalificacionClassroom(tarea, alumno, req.calificacion(), profesor);
            } catch (Exception e) {
                LOG.error(
                        "Error al sincronizar calificación con Classroom para entrega id={}: {}",
                        entregaId,
                        e.getMessage(),
                        e);
            }
        }

        return toEntregaResponse(entrega);
    }

    private void sincronizarCalificacionClassroom(
            TareaClassroom tarea, Usuario alumno, Integer calificacion, Usuario profesor) {

        Optional<ComunidadClassroom> vinculacion =
                googleClassroomService.getVinculacion(tarea.getComunidad().getId());
        if (vinculacion.isEmpty()) {
            return;
        }

        try {
            String accessToken = googleClassroomService.getAccessTokenValido(profesor);
            String courseId = vinculacion.get().getClassroomCourseId();
            String courseWorkId = tarea.getClassroomCourseWorkId();

            String listUrl =
                    "https://classroom.googleapis.com/v1/courses/"
                            + courseId
                            + "/courseWork/"
                            + courseWorkId
                            + "/studentSubmissions";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> listReq = new HttpEntity<>(headers);

            ResponseEntity<String> listResp =
                    restTemplate.exchange(listUrl, HttpMethod.GET, listReq, String.class);

            if (!listResp.getStatusCode().is2xxSuccessful() || listResp.getBody() == null) {
                return;
            }

            JsonNode listJson = objectMapper.readTree(listResp.getBody());
            JsonNode submissions = listJson.path("studentSubmissions");
            if (!submissions.isArray()) {
                return;
            }

            String submissionId = null;
            for (JsonNode sub : submissions) {
                JsonNode userIdNode = sub.path("userId");
                if (!userIdNode.isMissingNode() && userIdNode.asText().equals(alumno.getEmail())) {
                    submissionId = sub.path("id").asText();
                    break;
                }
            }

            if (submissionId == null) {
                return;
            }

            String patchUrl =
                    "https://classroom.googleapis.com/v1/courses/"
                            + courseId
                            + "/courseWork/"
                            + courseWorkId
                            + "/studentSubmissions/"
                            + submissionId
                            + "?updateMask=assignedGrade";

            String patchBody = String.format("{\"assignedGrade\":%d}", calificacion);

            HttpHeaders patchHeaders = new HttpHeaders();
            patchHeaders.setBearerAuth(accessToken);
            patchHeaders.set("Content-Type", "application/json");
            HttpEntity<String> patchReq = new HttpEntity<>(patchBody, patchHeaders);

            restTemplate.exchange(patchUrl, HttpMethod.PATCH, patchReq, String.class);
        } catch (Exception e) {
            LOG.error("Error al sincronizar calificación en Classroom: {}", e.getMessage(), e);
        }
    }

    /** Devuelve la entrega del alumno autenticado para una tarea, o null si no existe. */
    @Transactional(readOnly = true)
    public EntregaTareaResponse getMiEntrega(Long tareaId, Usuario alumno) {
        return entregaTareaRepository
                .findByTareaIdAndAlumnoId(tareaId, alumno.getId())
                .map(this::toEntregaResponse)
                .orElse(null);
    }

    /** Devuelve todas las entregas del alumno en una comunidad. */
    @Transactional(readOnly = true)
    public List<EntregaTareaResponse> getMisEntregasEnComunidad(Long comunidadId, Usuario alumno) {
        return entregaTareaRepository
                .findEntregasAlumnoEnComunidad(comunidadId, alumno.getId())
                .stream()
                .map(this::toEntregaResponse)
                .toList();
    }

    private TareaClassroomResponse toResponse(TareaClassroom t) {
        return new TareaClassroomResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getFechaVencimiento(),
                t.getPuntosMaximos(),
                t.getPublicada(),
                t.getClassroomCourseWorkId(),
                t.getEstado(),
                t.getCreadaPor() != null ? t.getCreadaPor().getId() : null,
                t.getCreadaPor() != null ? t.getCreadaPor().getNombre() : null,
                t.getCreatedAt(),
                t.getEntregas() != null ? t.getEntregas().size() : 0L);
    }

    private EntregaTareaResponse toEntregaResponse(EntregaTarea e) {
        return new EntregaTareaResponse(
                e.getId(),
                e.getTarea() != null ? e.getTarea().getId() : null,
                e.getTarea() != null ? e.getTarea().getTitulo() : null,
                e.getAlumno() != null ? e.getAlumno().getId() : null,
                e.getAlumno() != null ? e.getAlumno().getNombre() : null,
                e.getUrlArchivo(),
                e.getContenido(),
                e.getEstado(),
                e.getCalificacion(),
                e.getComentarioRetroalimentacion(),
                e.getSincronizadoClassroom(),
                e.getCreatedAt(),
                e.getFechaCalificacion());
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
