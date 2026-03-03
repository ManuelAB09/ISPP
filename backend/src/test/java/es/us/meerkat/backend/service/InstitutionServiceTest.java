package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock private InstitutionRepository institutionRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private InstitutionService institutionService;

    @BeforeEach
    void setUp() {
        // Setup
    }

    @Test
    void institutionServiceShouldBeInstantiated() {
        assertThat(institutionService).isNotNull();
    }

    @Test
    void obtenerInstitucionShouldReturnInstitutionWhenUserIsAdmin() {
        Long institutionId = 1L;
        Long usuarioId = 1L;
        Institution institution = buildInstitution(institutionId, usuarioId);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));

        Institution result = institutionService.obtenerInstitucion(institutionId, usuarioId);

        assertThat(result).isEqualTo(institution);
        verify(institutionRepository, times(1)).findById(institutionId);
    }

    @Test
    void obtenerInstitucionShouldFailWhenUserIsNotAdmin() {
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
        institution.setDominioEmail("example.com");

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institutionRepository.countUsuariosByDominioEmail("example.com")).thenReturn(5L);

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

    // Helper methods
    private Usuario buildUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Admin User");
        usuario.setEmail("admin@test.com");
        usuario.setPassword("password");
        return usuario;
    }

    private Institution buildInstitution(Long id, Long usuarioAdminId) {
        Usuario admin = buildUsuario(usuarioAdminId);
        Institution institution = new Institution();
        institution.setId(id);
        institution.setNombre("Test Institution");
        institution.setDescripcion("Test Description");
        institution.setDominioEmail("test.com");
        institution.setEmailContacto("contact@test.com");
        institution.setUsuarioAdmin(admin);
        institution.setCreatedAt(LocalDateTime.now());
        return institution;
    }
}
