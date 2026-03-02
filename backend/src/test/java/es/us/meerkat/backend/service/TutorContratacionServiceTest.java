package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.dto.HireTutorRequest;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoContratacion;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.TutorContratacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.TutorContratacionRepository;
import es.us.meerkat.backend.repository.TutorRepository;

@ExtendWith(MockitoExtension.class)
class TutorContratacionServiceTest {

    @Mock private TutorContratacionRepository tutorContratacionRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks private TutorContratacionService tutorContratacionService;

    @Test
    void crearContratacionShouldCreateAndReturnPaymentUrl() throws Exception {
        Long comunidadId = 10L;
        Long tutorId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(comunidadId, usuarioId);
        Tutor tutor = buildTutor(tutorId, true);
        HireTutorRequest request = new HireTutorRequest();
        request.setModalidad("Online");
        request.setDuracion("3 meses");
        request.setTarifaAcordada(new BigDecimal("50.00"));

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.empty());
        when(tutorContratacionRepository.save(any(TutorContratacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentService.generarPagoContratacionTutor(
                        tutorId, comunidadId, request.getTarifaAcordada(), usuarioId))
                .thenReturn(new PaymentUrlResponse("http://payment.url", "session123"));

        PaymentUrlResponse response =
                tutorContratacionService.crearContratacion(
                        comunidadId, tutorId, request, usuarioId);

        assertThat(response).isNotNull();
        assertThat(response.paymentUrl()).isEqualTo("http://payment.url");
        verify(tutorContratacionRepository).save(any(TutorContratacion.class));
    }

    @Test
    void crearContratacionShouldFailWhenUserIsNotAdmin() {
        Long comunidadId = 10L;
        Long tutorId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(comunidadId, 999L); // Different admin
        HireTutorRequest request = new HireTutorRequest();

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, tutorId, request, usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    void crearContratacionShouldFailWhenTutorNotVerified() {
        Long comunidadId = 10L;
        Long tutorId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(comunidadId, usuarioId);
        Tutor tutor = buildTutor(tutorId, false); // Not verified
        HireTutorRequest request = new HireTutorRequest();

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, tutorId, request, usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificado");
    }

    @Test
    void crearContratacionShouldFailWhenCommunityAlreadyHasActiveTutor() {
        Long comunidadId = 10L;
        Long tutorId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(comunidadId, usuarioId);
        Tutor tutor = buildTutor(tutorId, true);
        HireTutorRequest request = new HireTutorRequest();
        TutorContratacion existingContratacion = new TutorContratacion();

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.of(existingContratacion));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, tutorId, request, usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un tutor activo");
    }

    @Test
    void obtenerContratacionesDelTutorShouldReturnPageOfContrataciones() {
        Long tutorId = 1L;
        when(tutorContratacionRepository.findByTutorId(tutorId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var result =
                tutorContratacionService.obtenerContratacionesDelTutor(
                        tutorId, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(tutorContratacionRepository).findByTutorId(tutorId, PageRequest.of(0, 10));
    }

    @Test
    void obtenerContratacionesDeLaComunidadShouldReturnPageOfContrataciones() {
        Long comunidadId = 10L;
        when(tutorContratacionRepository.findByComunidadId(comunidadId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var result =
                tutorContratacionService.obtenerContratacionesDeLaComunidad(
                        comunidadId, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(tutorContratacionRepository).findByComunidadId(comunidadId, PageRequest.of(0, 10));
    }

    @Test
    void obtenerContratacionActivaDeComunidadShouldReturnActiveContratacion() {
        Long comunidadId = 10L;
        TutorContratacion contratacion = new TutorContratacion();
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.of(contratacion));

        var result = tutorContratacionService.obtenerContratacionActivaDeComunidad(comunidadId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(contratacion);
    }

    @Test
    void obtenerContratacionShouldReturnContratacion() {
        Long contratacionId = 1L;
        TutorContratacion contratacion = new TutorContratacion();
        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));

        var result = tutorContratacionService.obtenerContratacion(contratacionId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(contratacion);
    }

    @Test
    void activarContratacionShouldChangeStateToActive() {
        Long contratacionId = 1L;
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setId(contratacionId);
        contratacion.setEstado(EstadoContratacion.PENDIENTE_PAGO);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));
        when(tutorContratacionRepository.save(any(TutorContratacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorContratacionService.activarContratacion(contratacionId);

        ArgumentCaptor<TutorContratacion> captor = ArgumentCaptor.forClass(TutorContratacion.class);
        verify(tutorContratacionRepository).save(captor.capture());

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoContratacion.ACTIVA);
    }

    @Test
    void activarContratacionShouldFailWhenNotPendingPayment() {
        Long contratacionId = 1L;
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setEstado(EstadoContratacion.ACTIVA);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));

        assertThatThrownBy(() -> tutorContratacionService.activarContratacion(contratacionId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelarContratacionShouldChangeStateToCanceled() {
        Long contratacionId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(10L, usuarioId);
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setId(contratacionId);
        contratacion.setComunidad(comunidad);
        contratacion.setEstado(EstadoContratacion.ACTIVA);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));
        when(tutorContratacionRepository.save(any(TutorContratacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorContratacionService.cancelarContratacion(
                contratacionId, usuarioId, "Cambio de planes");

        ArgumentCaptor<TutorContratacion> captor = ArgumentCaptor.forClass(TutorContratacion.class);
        verify(tutorContratacionRepository).save(captor.capture());

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoContratacion.CANCELADA);
    }

    @Test
    void cancelarContratacionShouldFailWhenUserIsNotAdmin() {
        Long contratacionId = 1L;
        Long usuarioId = 20L;
        Comunidad comunidad = buildComunidad(10L, 999L); // Different admin
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setComunidad(comunidad);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.cancelarContratacion(
                                        contratacionId, usuarioId, "Motivo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completarContratacionShouldChangeStateToCompleted() {
        Long contratacionId = 1L;
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setId(contratacionId);
        contratacion.setEstado(EstadoContratacion.ACTIVA);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));
        when(tutorContratacionRepository.save(any(TutorContratacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorContratacionService.completarContratacion(contratacionId);

        ArgumentCaptor<TutorContratacion> captor = ArgumentCaptor.forClass(TutorContratacion.class);
        verify(tutorContratacionRepository).save(captor.capture());

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoContratacion.COMPLETADA);
    }

    @Test
    void tieneTutorActivoShouldReturnTrueWhenTutorExists() {
        Long comunidadId = 10L;
        TutorContratacion contratacion = new TutorContratacion();

        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.of(contratacion));

        boolean tiene = tutorContratacionService.tieneTutorActivo(comunidadId);

        assertThat(tiene).isTrue();
    }

    @Test
    void tieneTutorActivoShouldReturnFalseWhenTutorNotExists() {
        Long comunidadId = 10L;

        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.empty());

        boolean tiene = tutorContratacionService.tieneTutorActivo(comunidadId);

        assertThat(tiene).isFalse();
    }

    @Test
    void obtenerTutorActivoDeComunidadShouldReturnTutor() {
        Long comunidadId = 10L;
        Tutor tutor = buildTutor(1L, true);
        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setTutor(tutor);

        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.of(contratacion));

        var result = tutorContratacionService.obtenerTutorActivoDeComunidad(comunidadId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(tutor);
    }

    // Helper methods
    private Comunidad buildComunidad(Long id, Long adminId) {
        Usuario admin = new Usuario();
        admin.setId(adminId);
        admin.setNombre("Admin");
        admin.setEmail("admin@test.com");
        Comunidad comunidad = new Comunidad();
        comunidad.setId(id);
        comunidad.setNombre("Test Comunidad");
        comunidad.setCreador(admin);
        return comunidad;
    }

    private Tutor buildTutor(Long id, boolean verificado) {
        Tutor tutor = new Tutor();
        tutor.setId(id);
        tutor.setVerificado(verificado);
        tutor.setTarifaHora(new BigDecimal("50.00"));
        return tutor;
    }
}
