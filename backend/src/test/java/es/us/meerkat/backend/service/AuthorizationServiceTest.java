package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private ComunidadRepository comunidadRepository;

    @InjectMocks private AuthorizationService authorizationService;

    @Test
    void isAdminOfShouldReturnTrueWhenUserIsAdmin() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.isAdminOf(1L, 10L)).isTrue();
    }

    @Test
    void isAdminOfShouldReturnFalseWhenUserIsMember() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.isAdminOf(1L, 10L)).isFalse();
    }

    @Test
    void isAdminOfShouldReturnFalseWhenUserIsNotMember() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThat(authorizationService.isAdminOf(1L, 10L)).isFalse();
    }

    @Test
    void isMemberOfShouldReturnTrueWhenMembershipExists() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.isMemberOf(1L, 10L)).isTrue();
    }

    @Test
    void isMemberOfShouldReturnFalseWhenNoMembership() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThat(authorizationService.isMemberOf(1L, 10L)).isFalse();
    }

    @Test
    void getUserRoleInCommunityShouldReturnRolWhenMember() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.getUserRoleInCommunity(1L, 10L))
                .isEqualTo(RolComunidad.ADMIN);
    }

    @Test
    void getUserRoleInCommunityShouldReturnNullWhenNotMember() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThat(authorizationService.getUserRoleInCommunity(1L, 10L)).isNull();
    }

    @Test
    void getMembershipShouldReturnMiembroWhenExists() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.getMembership(1L, 10L)).isEqualTo(miembro);
    }

    @Test
    void getMembershipShouldReturnNullWhenNotMember() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThat(authorizationService.getMembership(1L, 10L)).isNull();
    }

    @Test
    void getUserRoleInCommunityAsStringShouldReturnRolNameWhenMember() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        assertThat(authorizationService.getUserRoleInCommunityAsString(1L, 10L)).isEqualTo("ADMIN");
    }

    @Test
    void getUserRoleInCommunityAsStringShouldReturnNullWhenNotMember() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThat(authorizationService.getUserRoleInCommunityAsString(1L, 10L)).isNull();
    }
}
