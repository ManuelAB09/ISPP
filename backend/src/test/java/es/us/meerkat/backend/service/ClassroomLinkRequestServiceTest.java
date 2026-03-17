package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.ClassroomLinkRequestResponse;
import es.us.meerkat.backend.entity.ClassroomLinkRequest;
import es.us.meerkat.backend.entity.ClassroomLinkRequestStatus;
import es.us.meerkat.backend.repository.ClassroomLinkRequestRepository;

@ExtendWith(MockitoExtension.class)
class ClassroomLinkRequestServiceTest {

    @Mock private ClassroomLinkRequestRepository repository;
    @Mock private GoogleClassroomService googleClassroomService;

    @InjectMocks private ClassroomLinkRequestService service;

    @Test
    void crearSolicitudShouldCreatePendingRequest() {
        when(repository.existsByComunidadIdAndTutorIdAndEstado(
                        10L, 1L, ClassroomLinkRequestStatus.PENDIENTE))
                .thenReturn(false);
        when(repository.save(any(ClassroomLinkRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ClassroomLinkRequest result = service.crearSolicitud(10L, 1L);

        assertThat(result.getComunidadId()).isEqualTo(10L);
        assertThat(result.getTutorId()).isEqualTo(1L);
        assertThat(result.getEstado()).isEqualTo(ClassroomLinkRequestStatus.PENDIENTE);
        assertThat(result.getFechaCreacion()).isNotNull();
    }

    @Test
    void crearSolicitudShouldThrowWhenPendingRequestExists() {
        when(repository.existsByComunidadIdAndTutorIdAndEstado(
                        10L, 1L, ClassroomLinkRequestStatus.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crearSolicitud(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una solicitud pendiente");
    }

    @Test
    void listarSolicitudesPendientesShouldReturnMappedResponses() {
        ClassroomLinkRequest req =
                ClassroomLinkRequest.builder()
                        .id(1L)
                        .comunidadId(10L)
                        .tutorId(5L)
                        .estado(ClassroomLinkRequestStatus.PENDIENTE)
                        .fechaCreacion(LocalDateTime.of(2025, 3, 15, 10, 0))
                        .build();

        when(repository.findByTutorIdAndEstado(5L, ClassroomLinkRequestStatus.PENDIENTE))
                .thenReturn(List.of(req));

        List<ClassroomLinkRequestResponse> result = service.listarSolicitudesPendientes(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).comunidadId()).isEqualTo(10L);
        assertThat(result.get(0).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void listarSolicitudesPendientesShouldReturnEmptyWhenNone() {
        when(repository.findByTutorIdAndEstado(5L, ClassroomLinkRequestStatus.PENDIENTE))
                .thenReturn(List.of());

        List<ClassroomLinkRequestResponse> result = service.listarSolicitudesPendientes(5L);

        assertThat(result).isEmpty();
    }

    @Test
    void completarSolicitudShouldLinkCourseAndUpdateStatus() {
        ClassroomLinkRequest solicitud =
                ClassroomLinkRequest.builder()
                        .id(1L)
                        .comunidadId(10L)
                        .tutorId(5L)
                        .estado(ClassroomLinkRequestStatus.PENDIENTE)
                        .build();

        when(repository.findByIdAndTutorId(1L, 5L)).thenReturn(Optional.of(solicitud));

        service.completarSolicitud(1L, 5L, "course123", "Math 101");

        verify(googleClassroomService).vincularCurso(10L, "course123", "Math 101");
        assertThat(solicitud.getEstado()).isEqualTo(ClassroomLinkRequestStatus.COMPLETADA);
        assertThat(solicitud.getFechaActualizacion()).isNotNull();
        verify(repository).save(solicitud);
    }

    @Test
    void completarSolicitudShouldThrowWhenNotFound() {
        when(repository.findByIdAndTutorId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completarSolicitud(1L, 5L, "c", "n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solicitud no encontrada");
    }

    @Test
    void completarSolicitudShouldThrowWhenNotPending() {
        ClassroomLinkRequest solicitud =
                ClassroomLinkRequest.builder()
                        .id(1L)
                        .comunidadId(10L)
                        .tutorId(5L)
                        .estado(ClassroomLinkRequestStatus.COMPLETADA)
                        .build();

        when(repository.findByIdAndTutorId(1L, 5L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.completarSolicitud(1L, 5L, "c", "n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solicitudes pendientes");
    }
}
