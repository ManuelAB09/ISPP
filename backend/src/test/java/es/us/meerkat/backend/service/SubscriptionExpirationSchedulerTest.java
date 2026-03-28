package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.suscriptions.SubscriptionExpirationScheduler;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationSchedulerTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private SubscriptionExpirationScheduler scheduler;

    @Test
    void desactivarSuscripcionesExpiradasShouldDeactivateExpiredSubscriptions() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPlan(TipoPlan.PREMIUM);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setActiva(true);
        suscripcion.setAutoRenovar(true);
        suscripcion.setPlan(TipoPlan.PREMIUM);
        suscripcion.setUsuario(usuario);

        when(suscripcionRepository.findByActivaTrueAndFechaFinBefore(any(LocalDate.class)))
                .thenReturn(List.of(suscripcion));

        scheduler.desactivarSuscripcionesExpiradas();

        assertThat(suscripcion.getActiva()).isFalse();
        assertThat(suscripcion.getAutoRenovar()).isFalse();
        assertThat(suscripcion.getPlan()).isEqualTo(TipoPlan.FREE);
        assertThat(usuario.getPlan()).isEqualTo(TipoPlan.FREE);
        verify(suscripcionRepository).save(suscripcion);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void desactivarSuscripcionesExpiradasShouldDoNothingWhenNoExpired() {
        when(suscripcionRepository.findByActivaTrueAndFechaFinBefore(any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.desactivarSuscripcionesExpiradas();

        verify(suscripcionRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarSuscripcionesExpiradasShouldNotUpdateUserIfAlreadyFree() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPlan(TipoPlan.FREE);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setActiva(true);
        suscripcion.setPlan(TipoPlan.PREMIUM);
        suscripcion.setUsuario(usuario);

        when(suscripcionRepository.findByActivaTrueAndFechaFinBefore(any(LocalDate.class)))
                .thenReturn(List.of(suscripcion));

        scheduler.desactivarSuscripcionesExpiradas();

        verify(suscripcionRepository).save(suscripcion);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarPlanesInstitucionalExpiradosShouldDeactivateExpiredPlans() {
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNombre("Test University");
        institution.setPlanActivo(true);

        when(institutionRepository.findByPlanActivoTrueAndFechaFinPlanBefore(
                        any(LocalDateTime.class)))
                .thenReturn(List.of(institution));

        scheduler.desactivarPlanesInstitucionalExpirados();

        assertThat(institution.getPlanActivo()).isFalse();
        verify(institutionRepository).save(institution);
    }

    @Test
    void desactivarPlanesInstitucionalExpiradosShouldDoNothingWhenNoExpired() {
        when(institutionRepository.findByPlanActivoTrueAndFechaFinPlanBefore(
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.desactivarPlanesInstitucionalExpirados();

        verify(institutionRepository, never()).save(any());
    }
}
