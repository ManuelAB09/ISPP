package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.tutors.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.tutors.DisponibilidadTutorResponse;
import es.us.meerkat.backend.entity.DisponibilidadTutor;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.tutors.DisponibilidadTutorRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.service.tutors.DisponibilidadService;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock private DisponibilidadTutorRepository disponibilidadRepository;

    @Mock private TutorRepository tutorRepository;

    @InjectMocks private DisponibilidadService disponibilidadService;

    @Test
    void crearDisponibilidadShouldThrowWhenTutorNotFound() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        when(tutorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tutor no encontrado");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenUserIsNotTutorOwner() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30));
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(99L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permisos para modificar disponibilidades de otro tutor");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenStartTimeAfterEndTime() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 12, 0));
        request.setHoraInicio(LocalTime.of(14, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hora de inicio debe ser anterior a la de fin");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenRecurrentButNoDayOfWeek() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(true);
        request.setDiaSemana(null);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Día de semana requerido para disponibilidad recurrente");
    }

    @Test
    void crearDisponibilidadShouldSuccessfullyCreateNonRecurrentDisponibilidad() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30));
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));
        request.setModalidad("PRESENCIAL");
        request.setUbicacionPresencial("Sevilla");

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disponibilidad =
                DisponibilidadTutor.builder()
                        .id(1L)
                        .tutor(tutor)
                        .esRecurrente(false)
                        .fechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .modalidad("PRESENCIAL")
                        .ubicacionPresencial("Sevilla")
                        .activa(true)
                        .build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class)))
                .thenReturn(disponibilidad);

        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(1L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(disponibilidadRepository).save(any(DisponibilidadTutor.class));
    }

    @Test
    void crearDisponibilidadShouldSuccessfullyCreateRecurrentDisponibilidad() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(true);
        request.setDiaSemana(DayOfWeek.MONDAY);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));
        request.setModalidad("VIRTUAL");

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disponibilidad =
                DisponibilidadTutor.builder()
                        .id(2L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .modalidad("VIRTUAL")
                        .activa(true)
                        .build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class)))
                .thenReturn(disponibilidad);

        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(1L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        verify(disponibilidadRepository).save(any(DisponibilidadTutor.class));
    }

    @Test
    void getDisponibilidadesShouldReturnAllActiveDisponibilidades() {
        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disp1 =
                DisponibilidadTutor.builder()
                        .id(1L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .activa(true)
                        .build();

        DisponibilidadTutor disp2 =
                DisponibilidadTutor.builder()
                        .id(2L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.TUESDAY)
                        .horaInicio(LocalTime.of(14, 0))
                        .horaFin(LocalTime.of(15, 0))
                        .activa(true)
                        .build();

        when(disponibilidadRepository.findByTutorIdAndActivaTrue(1L))
                .thenReturn(List.of(disp1, disp2));

        List<DisponibilidadTutorResponse> responses = disponibilidadService.getDisponibilidades(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
    }
}
