package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.ClassroomLinkRequest;
import es.us.meerkat.backend.entity.ClassroomLinkRequestStatus;
import es.us.meerkat.backend.repository.ClassroomLinkRequestRepository;

@ExtendWith(MockitoExtension.class)
class ClassroomLinkRequestServiceTest {

    @Mock private ClassroomLinkRequestRepository repository;
    @Mock private GoogleClassroomService googleClassroomService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private ClassroomLinkRequestService service;

    @Test
    void completarSolicitudShouldCompleteWhenTutor() {
        ClassroomLinkRequest s = new ClassroomLinkRequest();
        s.setId(1L);
        s.setComunidadId(5L);
        s.setTutorId(10L);
        s.setEstado(ClassroomLinkRequestStatus.PENDIENTE);
        s.setFechaCreacion(LocalDateTime.now());

        when(repository.findByIdAndTutorId(1L, 10L)).thenReturn(Optional.of(s));

        service.completarSolicitud(1L, 10L, "courseX", "Curso X");

        verify(googleClassroomService).vincularCurso(5L, "courseX", "Curso X");
        verify(repository).save(s);
    }

    @Test
    void completarSolicitudShouldAllowWhenAdminOrProfesor() {
        ClassroomLinkRequest s = new ClassroomLinkRequest();
        s.setId(2L);
        s.setComunidadId(8L);
        s.setTutorId(20L);
        s.setEstado(ClassroomLinkRequestStatus.PENDIENTE);

        // Simulate admin user (id 99) completing — mock repository to return the request
        when(repository.findByIdAndTutorId(2L, 99L)).thenReturn(Optional.of(s));
        when(authorizationService.isAdminOrProfesor(99L, 8L)).thenReturn(true);

        service.completarSolicitud(2L, 99L, "courseY", "Curso Y");

        verify(googleClassroomService).vincularCurso(8L, "courseY", "Curso Y");
        verify(repository).save(s);
    }

    @Test
    void completarSolicitudShouldFailWhenNotAuthorized() {
        ClassroomLinkRequest s = new ClassroomLinkRequest();
        s.setId(3L);
        s.setComunidadId(11L);
        s.setTutorId(30L);
        s.setEstado(ClassroomLinkRequestStatus.PENDIENTE);

        when(repository.findByIdAndTutorId(3L, 77L)).thenReturn(Optional.of(s));
        when(authorizationService.isAdminOrProfesor(77L, 11L)).thenReturn(false);

        assertThatThrownBy(() -> service.completarSolicitud(3L, 77L, "courseZ", "Curso Z"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No autorizado");
    }
}
