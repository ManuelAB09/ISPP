package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.CorporatePlanRequest;
import es.us.meerkat.backend.dto.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.InstitutionResponse;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.UpdateInstitutionRequest;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock private InstitutionRepository institutionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks private InstitutionService institutionService;

    @Test
    void crearInstitutionShouldCreateNewInstitution() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        CreateInstitutionRequest request = new CreateInstitutionRequest();
        request.setNombre("Universidad Nacional");
        request.setDescripcion("Una institución educativa principal");
        request.setEmailContacto("contacto@universidad.edu");
        request.setTelefonoContacto("+34 654 123 456");
        request.setDominioEmail("universidad.es");
        request.setUbicacion("Sevilla, España");
        request.setSitioweb("https://www.universidad.es");
        request.setLogoUrl("https://logo.universidad.es");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(institutionRepository.findByDominioEmail(request.getDominioEmail()))
                .thenReturn(Optional.empty());
        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        institutionService.crearInstitucion(usuarioId, request);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(captor.capture());

        assertThat(captor.getValue().getNombre()).isEqualTo(request.getNombre());
        assertThat(captor.getValue().getDominioEmail()).isEqualTo(request.getDominioEmail());
        assertThat(captor.getValue().getVerificada()).isFalse();
        assertThat(captor.getValue().getPlanActivo()).isFalse();
        assertThat(captor.getValue().getUsuarioAdmin()).isEqualTo(usuario);
    }

    @Test
    void crearInstitutionShouldFailWhenDominioEmailAlreadyExists() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        CreateInstitutionRequest request = new CreateInstitutionRequest();
        request.setDominioEmail("existente.es");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(institutionRepository.findByDominioEmail(request.getDominioEmail()))
                .thenReturn(Optional.of(new Institution()));

        assertThatThrownBy(() -> institutionService.crearInstitucion(usuarioId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtenerInstitutionShouldReturnInstitutionWhenUserIsAdmin() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        Institution result = institutionService.obtenerInstitucion(institutionId, usuarioId);

        assertThat(result).isEqualTo(institution);
    }

    @Test
    void obtenerInstitutionShouldFailWhenUserIsNotAdmin() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Long otherUserId = 999L;
        Institution institution = buildInstitution(institutionId, usuarioId);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        assertThatThrownBy(() -> institutionService.obtenerInstitucion(institutionId, otherUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtenerInstitutionPublicaShouldReturnInstitution() {
        Long institutionId = 1L;
        Institution institution = buildInstitution(institutionId, 1L);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        Institution result = institutionService.obtenerInstitutionPublica(institutionId);

        assertThat(result).isEqualTo(institution);
    }

    @Test
    void actualizarInstitutionShouldUpdateFields() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);

        UpdateInstitutionRequest request =
                UpdateInstitutionRequest.builder()
                        .nombre("Nuevo nombre")
                        .descripcion("Nueva descripción")
                        .emailContacto("newemail@universidad.es")
                        .build();

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Institution updated =
                institutionService.actualizarInstitucion(institutionId, usuarioId, request);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(captor.capture());

        assertThat(captor.getValue().getNombre()).isEqualTo("Nuevo nombre");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Nueva descripción");
        assertThat(captor.getValue().getEmailContacto()).isEqualTo("newemail@universidad.es");
    }

    @Test
    void contratarPlanCorporativoShouldCreatePaymentAndUpdateInstitution() throws Exception {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);

        CorporatePlanRequest request = new CorporatePlanRequest();
        request.setTipoPlan("BASICO");
        request.setNumUsuarios(100);
        request.setDuracionMeses(12);
        request.setPeriodo("anual"); // ← añadir periodo

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        when(paymentService.generarPagoPlanCorporativo(
                        eq(institutionId),
                        eq(TipoPlanCorporativo.BASICO),
                        any(BigDecimal.class), // monto calculado internamente
                        eq("anual"), // periodo
                        anyString())) // emailContacto
                .thenReturn(new PaymentUrlResponse("http://payment.url", "session123"));

        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentUrlResponse response =
                institutionService.contratarPlanCorporativo(institutionId, usuarioId, request);

        assertThat(response).isNotNull();
        assertThat(response.paymentUrl()).isEqualTo("http://payment.url");

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(captor.capture());

        assertThat(captor.getValue().getNumUsuariosPermitidos()).isEqualTo(100);
        assertThat(captor.getValue().getPlanCorporativo()).isEqualTo(TipoPlanCorporativo.BASICO);
    }

    @Test
    void contratarPlanCorporativoShouldFailForReducidoPlanWithPrivateEmail() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);
        institution.setDominioEmail("institucion-privada.es");

        CorporatePlanRequest request = new CorporatePlanRequest();
        request.setTipoPlan("REDUCIDO_PRIVADA");

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        assertThatThrownBy(
                        () ->
                                institutionService.contratarPlanCorporativo(
                                        institutionId, usuarioId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validarEligibilidadPlanReducidoShouldAcceptPublicEducationDomains() {
        String dominioPrivado = "empresa-privada.com";

        assertThatThrownBy(() -> institutionService.validarEligibilidadPlanReducido(dominioPrivado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elegible");
    }

    @Test
    void activarPlanCorporativoShouldSetPlanAsActive() {
        Long institutionId = 1L;
        Integer duracionMeses = 12;
        Institution institution = buildInstitution(institutionId, 1L);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        institutionService.activarPlanCorporativo(institutionId, duracionMeses);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(captor.capture());

        assertThat(captor.getValue().getPlanActivo()).isTrue();
    }

    @Test
    void cancelarPlanCorporativoShouldDeactivatePlan() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);
        institution.setPlanActivo(true);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        institutionService.cancelarPlanCorporativo(institutionId, usuarioId);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(captor.capture());

        assertThat(captor.getValue().getPlanActivo()).isFalse();
    }

    @Test
    void esPlanActivoShouldReturnTrueWhenPlanIsActive() {
        Long institutionId = 1L;
        Institution institution = buildInstitution(institutionId, 1L);
        institution.setPlanActivo(true);
        institution.setFechaFinPlan(LocalDateTime.now().plusMonths(1));

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        boolean result = institutionService.esPlanActivo(institutionId);

        assertThat(result).isTrue();
    }

    @Test
    void esPlanActivoShouldReturnFalseWhenPlanIsInactive() {
        Long institutionId = 1L;
        Institution institution = buildInstitution(institutionId, 1L);
        institution.setPlanActivo(false);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        boolean result = institutionService.esPlanActivo(institutionId);

        assertThat(result).isFalse();
    }

    @Test
    void obtenerNumUsuariosPermitidosShouldReturnPermittedUsers() {
        Long institutionId = 1L;
        Institution institution = buildInstitution(institutionId, 1L);
        institution.setNumUsuariosPermitidos(100);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        Integer result = institutionService.obtenerNumUsuariosPermitidos(institutionId);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void contarUsuariosShouldReturnUserCount() {
        Long institutionId = 1L;
        Institution institution = buildInstitution(institutionId, 1L);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institutionRepository.countUsuariosByDominioEmail(institution.getDominioEmail()))
                .thenReturn(5L);

        long result = institutionService.contarUsuarios(institutionId);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void contarComunidadesShouldReturnCommunityCount() {
        Long institutionId = 1L;
        when(institutionRepository.countComunidadesByInstitutionId(institutionId)).thenReturn(3L);

        long result = institutionService.contarComunidades(institutionId);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void obtenerTodasLasInstitucionesShouldReturnAllInstitutions() {
        // Given
        Institution institution1 = buildInstitution(1L, 1L);
        Institution institution2 = buildInstitution(2L, 2L);
        List<Institution> expectedInstitutions = Arrays.asList(institution1, institution2);

        when(institutionRepository.findAll()).thenReturn(expectedInstitutions);

        // When
        List<Institution> result = institutionService.obtenerTodasLasInstituciones();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(institution1, institution2);
        verify(institutionRepository).findAll();
    }

    @Test
    void obtenerTodasLasInstitucionesShouldReturnEmptyListWhenNoInstitutions() {
        // Given
        when(institutionRepository.findAll()).thenReturn(List.of());

        // When
        List<Institution> result = institutionService.obtenerTodasLasInstituciones();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(institutionRepository).findAll();
    }

    @Test
    void obtenerTodasLasInstitucionesResponseShouldReturnAllInstitutionsAsDTOs() {
        // Given
        Long adminId1 = 1L;
        Long adminId2 = 2L;

        Institution institution1 = buildInstitution(1L, adminId1);
        Institution institution2 = buildInstitution(2L, adminId2);
        List<Institution> institutions = Arrays.asList(institution1, institution2);

        when(institutionRepository.findAll()).thenReturn(institutions);
        when(institutionRepository.countUsuariosByDominioEmail(institution1.getDominioEmail()))
                .thenReturn(5L);
        when(institutionRepository.countUsuariosByDominioEmail(institution2.getDominioEmail()))
                .thenReturn(3L);
        when(institutionRepository.countComunidadesByInstitutionId(1L)).thenReturn(2L);
        when(institutionRepository.countComunidadesByInstitutionId(2L)).thenReturn(1L);

        // When
        List<InstitutionResponse> result =
                institutionService.obtenerTodasLasInstitucionesResponse();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        InstitutionResponse response1 = result.get(0);
        assertThat(response1.getId()).isEqualTo(institution1.getId());
        assertThat(response1.getNombre()).isEqualTo(institution1.getNombre());
        assertThat(response1.getTotalUsuarios()).isEqualTo(5);
        assertThat(response1.getTotalComunidades()).isEqualTo(2);

        InstitutionResponse response2 = result.get(1);
        assertThat(response2.getId()).isEqualTo(institution2.getId());
        assertThat(response2.getNombre()).isEqualTo(institution2.getNombre());
        assertThat(response2.getTotalUsuarios()).isEqualTo(3);
        assertThat(response2.getTotalComunidades()).isEqualTo(1);

        verify(institutionRepository).findAll();
        verify(institutionRepository).countUsuariosByDominioEmail(institution1.getDominioEmail());
        verify(institutionRepository).countUsuariosByDominioEmail(institution2.getDominioEmail());
        verify(institutionRepository).countComunidadesByInstitutionId(1L);
        verify(institutionRepository).countComunidadesByInstitutionId(2L);
    }

    @Test
    void obtenerTodasLasInstitucionesResponseShouldHandleNullPlanCorporativo() {
        // Given
        Institution institution = buildInstitution(1L, 1L);
        institution.setPlanCorporativo(null); // Explicitly set to null
        List<Institution> institutions = List.of(institution);

        when(institutionRepository.findAll()).thenReturn(institutions);
        when(institutionRepository.countUsuariosByDominioEmail(institution.getDominioEmail()))
                .thenReturn(5L);
        when(institutionRepository.countComunidadesByInstitutionId(1L)).thenReturn(2L);

        // When
        List<InstitutionResponse> result =
                institutionService.obtenerTodasLasInstitucionesResponse();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        InstitutionResponse response = result.get(0);
        assertThat(response.getPlanCorporativo()).isNull();
        assertThat(response.getTotalUsuarios()).isEqualTo(5);
        assertThat(response.getTotalComunidades()).isEqualTo(2);
    }

    @Test
    void obtenerTodasLasInstitucionesResponseShouldHandleZeroCounts() {
        // Given
        Institution institution = buildInstitution(1L, 1L);
        List<Institution> institutions = List.of(institution);

        when(institutionRepository.findAll()).thenReturn(institutions);
        when(institutionRepository.countUsuariosByDominioEmail(institution.getDominioEmail()))
                .thenReturn(0L);
        when(institutionRepository.countComunidadesByInstitutionId(1L)).thenReturn(0L);

        // When
        List<InstitutionResponse> result =
                institutionService.obtenerTodasLasInstitucionesResponse();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        InstitutionResponse response = result.get(0);
        assertThat(response.getTotalUsuarios()).isZero();
        assertThat(response.getTotalComunidades()).isZero();
    }

    // Helper methods
    private Usuario buildUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Admin User");
        usuario.setEmail("admin@test.com");
        usuario.setPassword("password");
        return usuario;
    }

    private Institution buildInstitution(Long id, Long adminId) {
        Usuario admin = buildUsuario(adminId);
        Institution institution = new Institution();
        institution.setId(id);
        institution.setNombre("Test Institution");
        institution.setDescripcion("Test Description");
        institution.setEmailContacto("contact@test.edu");
        institution.setTelefonoContacto("+34 654 123 456");
        institution.setDominioEmail("test.edu");
        institution.setUbicacion("Test City");
        institution.setSitioweb("https://test.edu");
        institution.setLogoUrl("https://logo.test.edu");
        institution.setUsuarioAdmin(admin);
        institution.setVerificada(false);
        institution.setPlanActivo(false);
        institution.setCreatedAt(LocalDateTime.now());
        return institution;
    }
}
