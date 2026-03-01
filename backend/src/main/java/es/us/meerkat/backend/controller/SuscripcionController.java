package es.us.meerkat.backend.controller;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.SubscriptionResponse;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.SuscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Controlador REST para gestionar suscripciones de usuarios. */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "suscripciones-controller", description = "Gestión de suscripciones a planes")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    /**
     * Obtiene todos los planes de suscripción disponibles.
     *
     * @return Tipos de planes disponibles
     */
    @GetMapping("/plans")
    @Operation(
            summary = "Ver planes disponibles",
            description = "Devuelve la lista de planes de suscripción disponibles")
    public ResponseEntity<TipoPlan[]> obtenerPlanes() {
        TipoPlan[] planes = suscripcionService.obtenerPlanesDisponibles();
        return ResponseEntity.ok(planes);
    }

    /**
     * Obtiene la suscripción actual del usuario autenticado.
     *
     * @return Suscripción del usuario o plan FREE por defecto
     */
    @GetMapping("/me")
    @Operation(
            summary = "Obtener mi suscripción",
            description = "Devuelve la suscripción actual del usuario autenticado")
    public ResponseEntity<SubscriptionResponse> obtenerMiSuscripcion(
            @AuthenticationPrincipal final Usuario usuario) {
        Optional<Suscripcion> suscripcion = suscripcionService.obtenerMiSuscripcion(usuario.getId());

        if (suscripcion.isPresent()) {
            return ResponseEntity.ok(suscripcion.get().toDTO());
        } else {
            SubscriptionResponse freePlan = SubscriptionResponse.builder()
                    .id(null)
                    .plan(TipoPlan.FREE)
                    .fechaInicio(LocalDate.now())
                    .fechaFin(LocalDate.now())
                    .activa(true)
                    .autoRenovar(false)
                    .enPeriodoGracia(false)
                    .build();
            return ResponseEntity.ok(freePlan);
        }
    }

    /**
     * Suscribe al usuario autenticado a un plan Premium.
     *
     * @return Suscripción creada o URL de pago
     */
    @PostMapping("/me")
    @Operation(summary = "Suscribirse a Premium", description = "Inicia el proceso de suscripción a un plan Premium")
    public ResponseEntity<SubscriptionResponse> suscribirse(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            Suscripcion suscripcion = suscripcionService.suscribirse(usuario.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(suscripcion.toDTO());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancela la suscripción Premium del usuario autenticado.
     *
     * @return Confirmación de cancelación
     */
    @DeleteMapping("/me")
    @Operation(summary = "Cancelar suscripción", description = "Cancela la suscripción del usuario.\r\n"
            + //
            "        El acceso Premium se mantiene hasta la fecha de fin del"
            + " período actual.")
    public ResponseEntity<SubscriptionResponse> cancelarSuscripcion(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            Suscripcion cancelada = suscripcionService.cancelarSuscripcion(usuario.getId());
            return ResponseEntity.ok(cancelada.toDTO());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
