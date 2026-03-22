package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import es.us.meerkat.backend.dto.CalificarReservaRequest;
import es.us.meerkat.backend.dto.CreateReservaClaseRequest;
import es.us.meerkat.backend.entity.EstadoReserva;
import es.us.meerkat.backend.entity.ReservaClase;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ReservaClaseRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReservaClaseServiceTest {

    @Mock private ReservaClaseRepository reservaRepository;

    @Mock private TutorRepository tutorRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private PaymentService paymentService;

    @Mock private JavaMailSender mailSender;

    @Mock private DisponibilidadService disponibilidadService;

    @InjectMocks private ReservaClaseService reservaClaseService;

    @Test
    void crearReservaShouldThrowWhenTutorNotFound() {
        CreateReservaClaseRequest request = new CreateReservaClaseRequest();
        request.setFechaHora(LocalDateTime.now().plusDays(1));
        request.setDuracionMinutos(60);
        request.setModalidad("VIRTUAL");
        request.setTema("Java");

        when(tutorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaClaseService.crearReserva(1L, request, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tutor no encontrado");
    }

    @Test
    void cancelarReservaShouldThrowWhenReservationNotFound() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaClaseService.cancelarReserva(1L, 1L, "Motivo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reserva no encontrada");
    }

    @Test
    void confirmarReservaShouldThrowWhenNotFound() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaClaseService.confirmarReserva(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reserva no encontrada");
    }

    @Test
    void calificarReservaShouldSuccessfullyRateReservation() {
        CalificarReservaRequest request = new CalificarReservaRequest();
        request.setCalificacion(5);
        request.setComentario("Excelente clase");
        request.setAceptarTerminos(true);

        Usuario alumno = new Usuario();
        alumno.setId(1L);

        Usuario tutorUser = new Usuario();
        tutorUser.setId(2L);

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorUser);

        ReservaClase reserva =
                ReservaClase.builder()
                        .id(1L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .fechaHora(LocalDateTime.now().minusHours(1))
                        .duracionMinutos(60)
                        .modalidad("VIRTUAL")
                        .tema("Spring Boot")
                        .tarifa(BigDecimal.valueOf(35))
                        .estado(EstadoReserva.COMPLETADA)
                        .build();

        ReservaClase ratedReserva =
                ReservaClase.builder()
                        .id(1L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .fechaHora(LocalDateTime.now().minusHours(1))
                        .duracionMinutos(60)
                        .modalidad("VIRTUAL")
                        .tema("Spring Boot")
                        .tarifa(BigDecimal.valueOf(35))
                        .estado(EstadoReserva.COMPLETADA)
                        .calificacion(5)
                        .comentarioAlumno("Excelente clase")
                        .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(ReservaClase.class))).thenReturn(ratedReserva);

        reservaClaseService.calificarReserva(1L, request, 1L);

        verify(reservaRepository).save(any(ReservaClase.class));
    }

    @Test
    void reservaClaseServiceShouldBeInstantiated() {
        assertThat(reservaClaseService).isNotNull();
    }
}
