package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import es.us.meerkat.backend.dto.HireTutorRequest;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoContratacion;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
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
    @Mock private ClassroomLinkRequestService classroomLinkRequestService;

    @InjectMocks private TutorContratacionService tutorContratacionService;

    // =============================================
    // crearContratacion
    // =============================================

    @Test
    void createHiringShouldCreateContractAndReturnPaymentUrl() throws Exception {
        Long comunidadId = 1L;
        Long tutorId = 10L;
        Long usuarioId = 5L;
        Usuario creador = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(comunidadId, creador);
        Tutor tutor = buildTutorVerificado(tutorId, buildUsuario(20L));
        HireTutorRequest request = buildHireTutorRequest();

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.empty());
        when(tutorContratacionRepository.save(any(TutorContratacion.class)))
                .thenAnswer(
                        inv -> {
                            TutorContratacion tc = inv.getArgument(0);
                            tc.setId(100L);
                            return tc;
                        });
        when(paymentService.generarPagoContratacionTutor(
                        tutorId, comunidadId, request.getTarifaAcordada(), usuarioId))
                .thenReturn(new PaymentUrlResponse("https://stripe.com/pay/123", "sess_123"));

        PaymentUrlResponse response =
                tutorContratacionService.crearContratacion(
                        comunidadId, tutorId, request, usuarioId);

        assertThat(response.paymentUrl()).contains("stripe.com");

        ArgumentCaptor<TutorContratacion> captor = ArgumentCaptor.forClass(TutorContratacion.class);
        verify(tutorContratacionRepository).save(captor.capture());
        TutorContratacion saved = captor.getValue();
        assertThat(saved.getEstado()).isEqualTo(EstadoContratacion.PENDIENTE_PAGO);
        assertThat(saved.getTutor()).isEqualTo(tutor);
        assertThat(saved.getComunidad()).isEqualTo(comunidad);
        assertThat(saved.getModalidad()).isEqualTo("horaria");
    }

    @Test
    void createHiringShouldFailWhenCommunityNotFound() {
        when(comunidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        99L, 10L, buildHireTutorRequest(), 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comunidad no encontrada");
    }

    @Test
    void createHiringShouldFailWhenUserIsNotCommunityCreator() {
        Long comunidadId = 1L;
        Long otroUsuarioId = 999L;
        Comunidad comunidad = buildComunidad(comunidadId, buildUsuario(5L));

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, 10L, buildHireTutorRequest(), otroUsuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permisos");
    }

    @Test
    void createHiringShouldFailWhenCommunityAlreadyHasActiveTutor() {
        Long comunidadId = 1L;
        Long usuarioId = 5L;
        Usuario creador = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(comunidadId, creador);
        Tutor tutor = buildTutorVerificado(10L, buildUsuario(20L));

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.of(new TutorContratacion()));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, 10L, buildHireTutorRequest(), usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un tutor activo");
    }

    @Test
    void createHiringShouldFailWhenTutorIsNotVerified() {
        Long comunidadId = 1L;
        Long usuarioId = 5L;
        Usuario creador = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(comunidadId, creador);
        Tutor tutorNoVerificado = buildTutorVerificado(10L, buildUsuario(20L));
        tutorNoVerificado.setVerificado(false);

        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutorNoVerificado));
        when(tutorContratacionRepository.findActivaByComunidadId(comunidadId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.crearContratacion(
                                        comunidadId, 10L, buildHireTutorRequest(), usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificado");
    }

    // =============================================
    // activarContratacion
    // =============================================

    @Test
    void activateHiringShouldChangeStatusToActive() {
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.PENDIENTE_PAGO);
        Usuario tutorUser = buildUsuario(20L);
        Tutor tutor = buildTutorVerificado(10L, tutorUser);
        Comunidad comunidad = buildComunidad(1L, buildUsuario(5L));
        contratacion.setTutor(tutor);
        contratacion.setComunidad(comunidad);

        when(tutorContratacionRepository.findById(100L)).thenReturn(Optional.of(contratacion));

        tutorContratacionService.activarContratacion(100L);

        assertThat(contratacion.getEstado()).isEqualTo(EstadoContratacion.ACTIVA);
        assertThat(contratacion.getFechaInicio()).isEqualTo(LocalDate.now());
        verify(tutorContratacionRepository).save(contratacion);
        verify(classroomLinkRequestService).crearSolicitud(comunidad.getId(), tutorUser.getId());
    }

    @Test
    void activateHiringShouldFailWhenStatusIsNotPendingPayment() {
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.ACTIVA);

        when(tutorContratacionRepository.findById(100L)).thenReturn(Optional.of(contratacion));

        assertThatThrownBy(() -> tutorContratacionService.activarContratacion(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDIENTE_PAGO");
    }

    @Test
    void activateHiringShouldFailWhenNotFound() {
        when(tutorContratacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorContratacionService.activarContratacion(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    // =============================================
    // cancelarContratacion
    // =============================================

    @Test
    void cancelHiringShouldCancelSuccessfully() {
        Long contratacionId = 100L;
        Long usuarioId = 5L;
        Usuario creador = buildUsuario(usuarioId);
        Comunidad comunidad = buildComunidad(1L, creador);

        TutorContratacion contratacion =
                buildContratacion(contratacionId, EstadoContratacion.ACTIVA);
        contratacion.setComunidad(comunidad);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));

        tutorContratacionService.cancelarContratacion(
                contratacionId, usuarioId, "Ya no lo necesitamos");

        assertThat(contratacion.getEstado()).isEqualTo(EstadoContratacion.CANCELADA);
        assertThat(contratacion.getMotivoCancelacion()).isEqualTo("Ya no lo necesitamos");
        assertThat(contratacion.getFechaFin()).isEqualTo(LocalDate.now());
        verify(tutorContratacionRepository).save(contratacion);
    }

    @Test
    void cancelHiringShouldFailWhenUserIsNotCreator() {
        Long contratacionId = 100L;
        Long otroUsuarioId = 999L;
        Usuario creador = buildUsuario(5L);
        Comunidad comunidad = buildComunidad(1L, creador);

        TutorContratacion contratacion =
                buildContratacion(contratacionId, EstadoContratacion.ACTIVA);
        contratacion.setComunidad(comunidad);

        when(tutorContratacionRepository.findById(contratacionId))
                .thenReturn(Optional.of(contratacion));

        assertThatThrownBy(
                        () ->
                                tutorContratacionService.cancelarContratacion(
                                        contratacionId, otroUsuarioId, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permisos");
    }

    // =============================================
    // completarContratacion
    // =============================================

    @Test
    void completeHiringShouldChangeStatusToCompleted() {
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.ACTIVA);

        when(tutorContratacionRepository.findById(100L)).thenReturn(Optional.of(contratacion));

        tutorContratacionService.completarContratacion(100L);

        assertThat(contratacion.getEstado()).isEqualTo(EstadoContratacion.COMPLETADA);
        assertThat(contratacion.getFechaFin()).isEqualTo(LocalDate.now());
        verify(tutorContratacionRepository).save(contratacion);
    }

    // =============================================
    // tieneTutorActivo
    // =============================================

    @Test
    void hasActiveTutorShouldReturnTrueWhenExists() {
        when(tutorContratacionRepository.findActivaByComunidadId(1L))
                .thenReturn(Optional.of(new TutorContratacion()));

        assertThat(tutorContratacionService.tieneTutorActivo(1L)).isTrue();
    }

    @Test
    void hasActiveTutorShouldReturnFalseWhenNotExists() {
        when(tutorContratacionRepository.findActivaByComunidadId(1L)).thenReturn(Optional.empty());

        assertThat(tutorContratacionService.tieneTutorActivo(1L)).isFalse();
    }

    // =============================================
    // obtenerContratacionesDelTutor
    // =============================================

    @Test
    void getTutorHiringsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TutorContratacion> page =
                new PageImpl<>(List.of(buildContratacion(1L, EstadoContratacion.ACTIVA)));

        when(tutorContratacionRepository.findByTutorId(10L, pageable)).thenReturn(page);

        Page<TutorContratacion> result =
                tutorContratacionService.obtenerContratacionesDelTutor(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // =============================================
    // obtenerContratacionesDeLaComunidad
    // =============================================

    @Test
    void getCommunityHiringsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TutorContratacion> page = new PageImpl<>(List.of());

        when(tutorContratacionRepository.findByComunidadId(1L, pageable)).thenReturn(page);

        Page<TutorContratacion> result =
                tutorContratacionService.obtenerContratacionesDeLaComunidad(1L, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // =============================================
    // obtenerContratacionActivaDeComunidad
    // =============================================

    @Test
    void getActiveHiringByCommunityShouldReturnHiring() {
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.ACTIVA);
        when(tutorContratacionRepository.findActivaByComunidadId(1L))
                .thenReturn(Optional.of(contratacion));

        Optional<TutorContratacion> result =
                tutorContratacionService.obtenerContratacionActivaDeComunidad(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getEstado()).isEqualTo(EstadoContratacion.ACTIVA);
    }

    // =============================================
    // obtenerContratacion
    // =============================================

    @Test
    void getHiringByIdShouldReturnHiring() {
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.COMPLETADA);
        when(tutorContratacionRepository.findById(100L)).thenReturn(Optional.of(contratacion));

        Optional<TutorContratacion> result = tutorContratacionService.obtenerContratacion(100L);

        assertThat(result).isPresent();
    }

    // =============================================
    // obtenerTutorActivoDeComunidad
    // =============================================

    @Test
    void getActiveTutorByCcommunityShouldReturnTutor() {
        Tutor tutor = buildTutorVerificado(10L, buildUsuario(20L));
        TutorContratacion contratacion = buildContratacion(100L, EstadoContratacion.ACTIVA);
        contratacion.setTutor(tutor);

        when(tutorContratacionRepository.findActivaByComunidadId(1L))
                .thenReturn(Optional.of(contratacion));

        Optional<Tutor> result = tutorContratacionService.obtenerTutorActivoDeComunidad(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
    }

    @Test
    void getActiveTutorByCommunityShouldReturnEmptyWhenNoActive() {
        when(tutorContratacionRepository.findActivaByComunidadId(1L)).thenReturn(Optional.empty());

        Optional<Tutor> result = tutorContratacionService.obtenerTutorActivoDeComunidad(1L);

        assertThat(result).isEmpty();
    }

    // =============================================
    // Helpers
    // =============================================

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario " + id);
        u.setEmail("user" + id + "@meerkat.es");
        return u;
    }

    private Comunidad buildComunidad(Long id, Usuario creador) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad Test")
                .descripcion("Descripción")
                .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                .tipoPlan(TipoPlanComunidad.FREE)
                .maxMiembros(50)
                .creador(creador)
                .build();
    }

    private Tutor buildTutorVerificado(Long id, Usuario usuario) {
        Tutor t = new Tutor();
        t.setId(id);
        t.setUs(usuario);
        t.setEspecialidades(List.of("Matemáticas"));
        t.setTarifaHora(new BigDecimal("25.00"));
        t.setDisponibilidad("Tardes");
        t.setBio("Bio de prueba");
        t.setVerificado(true);
        t.setClassroomConectado(false);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private TutorContratacion buildContratacion(Long id, EstadoContratacion estado) {
        TutorContratacion tc = new TutorContratacion();
        tc.setId(id);
        tc.setEstado(estado);
        tc.setModalidad("horaria");
        tc.setDuracion("3 meses");
        tc.setTarifaAcordada(new BigDecimal("25.50"));
        tc.setFechaInicio(LocalDate.now());
        tc.setFechaFin(LocalDate.now().plusMonths(3));
        tc.setCreatedAt(LocalDateTime.now());
        return tc;
    }

    private HireTutorRequest buildHireTutorRequest() {
        return HireTutorRequest.builder()
                .modalidad("horaria")
                .duracion("3 meses")
                .tarifaAcordada(new BigDecimal("25.50"))
                .aceptarTerminos(true)
                .build();
    }
}
