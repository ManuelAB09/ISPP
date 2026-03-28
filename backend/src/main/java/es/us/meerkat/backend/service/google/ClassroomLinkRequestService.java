package es.us.meerkat.backend.service.google;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.google.ClassroomLinkRequestResponse;
import es.us.meerkat.backend.entity.ClassroomLinkRequest;
import es.us.meerkat.backend.entity.ClassroomLinkRequestStatus;
import es.us.meerkat.backend.repository.google.ClassroomLinkRequestRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomLinkRequestService {
    private final ClassroomLinkRequestRepository repository;
    private final GoogleClassroomService googleClassroomService;
    private final AuthorizationService authorizationService;

    @Transactional
    public ClassroomLinkRequest crearSolicitud(Long comunidadId, Long tutorId) {
        boolean exists =
                repository.existsByComunidadIdAndTutorIdAndEstado(
                        comunidadId, tutorId, ClassroomLinkRequestStatus.PENDIENTE);

        if (exists) {
            throw new IllegalStateException(
                    "Ya existe una solicitud pendiente para esta comunidad y tutor.");
        }

        ClassroomLinkRequest solicitud =
                ClassroomLinkRequest.builder()
                        .comunidadId(comunidadId)
                        .tutorId(tutorId)
                        .estado(ClassroomLinkRequestStatus.PENDIENTE)
                        .fechaCreacion(LocalDateTime.now())
                        .build();
        return repository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public List<ClassroomLinkRequestResponse> listarSolicitudesPendientes(Long tutorId) {
        return repository
                .findByTutorIdAndEstado(tutorId, ClassroomLinkRequestStatus.PENDIENTE)
                .stream()
                .map(
                        s ->
                                new ClassroomLinkRequestResponse(
                                        s.getId(),
                                        s.getComunidadId(),
                                        s.getTutorId(),
                                        s.getEstado().name(),
                                        s.getFechaCreacion()))
                .toList();
    }

    @Transactional
    public void completarSolicitud(
            Long solicitudId, Long tutorId, String cursoId, String nombreCurso) {
        ClassroomLinkRequest solicitud =
                repository
                        .findByIdAndTutorId(solicitudId, tutorId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Solicitud no encontrada o no pertenece al"
                                                        + " tutor."));

        if (solicitud.getEstado() != ClassroomLinkRequestStatus.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden completar solicitudes pendientes.");
        }

        Long comunidadId = solicitud.getComunidadId();

        boolean isTutor = solicitud.getTutorId() != null && solicitud.getTutorId().equals(tutorId);
        boolean isAdminOrProfesor = authorizationService.isAdminOrProfesor(tutorId, comunidadId);

        if (!isTutor && !isAdminOrProfesor) {
            throw new IllegalStateException("No autorizado para completar la solicitud.");
        }

        googleClassroomService.vincularCurso(comunidadId, cursoId, nombreCurso);

        solicitud.setEstado(ClassroomLinkRequestStatus.COMPLETADA);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        repository.save(solicitud);
    }
}
