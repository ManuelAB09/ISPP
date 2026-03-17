package es.us.meerkat.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que revisa y desactiva suscripciones y planes institucionales expirados. Se
 * ejecuta diariamente a las 2:00 AM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SuscripcionRepository suscripcionRepository;
    private final InstitutionRepository institutionRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Desactiva suscripciones individuales PREMIUM expiradas. Pone activa=false, plan=FREE en la
     * suscripción y en el usuario.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void desactivarSuscripcionesExpiradas() {
        List<Suscripcion> expiradas =
                suscripcionRepository.findByActivaTrueAndFechaFinBefore(LocalDate.now());

        if (expiradas.isEmpty()) {
            return;
        }

        log.info("Desactivando {} suscripciones individuales expiradas", expiradas.size());

        for (Suscripcion suscripcion : expiradas) {
            suscripcion.setActiva(false);
            suscripcion.setAutoRenovar(false);
            suscripcion.setPlan(TipoPlan.FREE);
            suscripcionRepository.save(suscripcion);

            // Revertir plan del usuario a FREE
            Usuario usuario = suscripcion.getUsuario();
            if (usuario != null && usuario.getPlan() == TipoPlan.PREMIUM) {
                usuario.setPlan(TipoPlan.FREE);
                usuarioRepository.save(usuario);
                log.info("Usuario {} revertido a FREE (suscripción expirada)", usuario.getId());
            }
        }

        log.info("Finalizada desactivación de suscripciones individuales expiradas");
    }

    /** Desactiva planes institucionales expirados. Pone planActivo=false en la institución. */
    @Scheduled(cron = "0 5 2 * * *")
    @Transactional
    public void desactivarPlanesInstitucionalExpirados() {
        List<Institution> expiradas =
                institutionRepository.findByPlanActivoTrueAndFechaFinPlanBefore(
                        LocalDateTime.now());

        if (expiradas.isEmpty()) {
            return;
        }

        log.info("Desactivando {} planes institucionales expirados", expiradas.size());

        for (Institution institution : expiradas) {
            institution.setPlanActivo(false);
            institutionRepository.save(institution);
            log.info(
                    "Plan institucional de '{}' (ID: {}) desactivado por expiración",
                    institution.getNombre(),
                    institution.getId());
        }

        log.info("Finalizada desactivación de planes institucionales expirados");
    }
}
