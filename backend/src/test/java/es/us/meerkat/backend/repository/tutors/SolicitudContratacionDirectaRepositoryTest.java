package es.us.meerkat.backend.repository.tutors;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import es.us.meerkat.backend.entity.tutors.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.tutors.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class SolicitudContratacionDirectaRepositoryTest {

    @Autowired private SolicitudContratacionDirectaRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario alumno;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        alumno = Usuario.builder().email("alumno@test.com").password("pass").build();
        em.persist(alumno);

        Usuario tutorUser = Usuario.builder().email("tutor@test.com").password("pass").build();
        em.persist(tutorUser);

        tutor = new Tutor();
        tutor.setUsuario(tutorUser);
        tutor.setNivelDesempeno("PRINCIPIANTE");
        tutor.setPuntuacionMedia(0.0);
        tutor.setTotalValoraciones(0);
        em.persist(tutor);

        em.flush();
    }

    private SolicitudContratacionDirecta build(
            LocalDate dia, LocalTime ini, LocalTime fin, EstadoSolicitudContratacion estado) {
        return SolicitudContratacionDirecta.builder()
                .alumno(alumno)
                .tutor(tutor)
                .dia(dia)
                .horaInicio(ini)
                .horaFin(fin)
                .tarifaHora(BigDecimal.TEN)
                .importeTotal(BigDecimal.TEN)
                .estado(estado)
                .build();
    }

    @Test
    void findPendientesByTutorId_returnsPendingOnly() {
        em.persist(
                build(
                        LocalDate.now().plusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PENDIENTE));
        em.persist(
                build(
                        LocalDate.now().plusDays(2),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findPendientesByTutorId(tutor.getId());

        assertThat(result)
                .hasSize(1)
                .allMatch(s -> s.getEstado() == EstadoSolicitudContratacion.PENDIENTE);
    }

    @Test
    void findConflictingBookings_overlappingSlot_returnsConflict() {
        LocalDate day = LocalDate.now().plusDays(5);
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findConflictingBookings(
                        tutor.getId(), day, LocalTime.of(11, 0), LocalTime.of(13, 0));

        assertThat(result).hasSize(1);
    }

    @Test
    void findConflictingBookings_noOverlap_returnsEmpty() {
        LocalDate day = LocalDate.now().plusDays(5);
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findConflictingBookings(
                        tutor.getId(), day, LocalTime.of(12, 0), LocalTime.of(13, 0));

        assertThat(result).isEmpty();
    }

    @Test
    void findConflictingBookingsExcluding_excludesSpecifiedId() {
        LocalDate day = LocalDate.now().plusDays(5);
        SolicitudContratacionDirecta s1 =
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        EstadoSolicitudContratacion.ACEPTADA);
        em.persist(s1);
        SolicitudContratacionDirecta s2 =
                build(
                        day,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        EstadoSolicitudContratacion.PAGADA);
        em.persist(s2);
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findConflictingBookingsExcluding(
                        tutor.getId(), day, LocalTime.of(10, 0), LocalTime.of(14, 0), s1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(s2.getId());
    }

    @Test
    void findConflictingBookingsAnyState_ignoresCancelledAndRejected() {
        LocalDate day = LocalDate.now().plusDays(5);
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        EstadoSolicitudContratacion.PENDIENTE));
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        EstadoSolicitudContratacion.CANCELADA_ALUMNO));
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        EstadoSolicitudContratacion.RECHAZADA));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findConflictingBookingsAnyState(
                        tutor.getId(), day, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(result)
                .hasSize(1)
                .allMatch(s -> s.getEstado() == EstadoSolicitudContratacion.PENDIENTE);
    }

    @Test
    void findBookingsForDate_returnsPaidOnDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        em.persist(
                build(
                        tomorrow,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PAGADA));
        em.persist(
                build(
                        tomorrow,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.persist(
                build(
                        LocalDate.now().plusDays(3),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PAGADA));
        em.flush();

        List<SolicitudContratacionDirecta> result = repository.findBookingsForDate(tomorrow);

        assertThat(result)
                .hasSize(1)
                .allMatch(s -> s.getEstado() == EstadoSolicitudContratacion.PAGADA)
                .allMatch(s -> s.getDia().equals(tomorrow));
    }

    @Test
    void findExpiredAcceptedBookings_returnsAcceptedBeforeDate() {
        LocalDate past = LocalDate.now().minusDays(5);
        em.persist(
                build(
                        past,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.persist(
                build(
                        LocalDate.now().plusDays(5),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.persist(
                build(
                        past,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PAGADA));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findExpiredAcceptedBookings(LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    void findExpiredPendingBookings_returnsPendingBeforeDate() {
        LocalDate past = LocalDate.now().minusDays(5);
        em.persist(
                build(
                        past,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PENDIENTE));
        em.persist(
                build(
                        LocalDate.now().plusDays(5),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.PENDIENTE));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findExpiredPendingBookings(LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    void findActiveBookingsByTutorAndDate_returnsOrderedByHoraInicio() {
        LocalDate day = LocalDate.now().plusDays(3);
        em.persist(
                build(
                        day,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        EstadoSolicitudContratacion.PAGADA));
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.ACEPTADA));
        em.persist(
                build(
                        day,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        EstadoSolicitudContratacion.CANCELADA_ALUMNO));
        em.flush();

        List<SolicitudContratacionDirecta> result =
                repository.findActiveBookingsByTutorAndDate(tutor.getId(), day);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHoraInicio()).isBefore(result.get(1).getHoraInicio());
    }
}
