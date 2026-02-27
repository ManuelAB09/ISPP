package es.us.meerkat.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para gestionar suscripciones de usuarios. */
@Service
@RequiredArgsConstructor
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene todos los planes disponibles.
     *
     * @return Array de tipos de planes
     */
    public TipoPlan[] obtenerPlanesDisponibles() {
        return TipoPlan.values();
    }

    /**
     * Obtiene la suscripción actual de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción activa si existe
     */
    public Optional<Suscripcion> obtenerMiSuscripcion(Long usuarioId) {
        return suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true);
    }

    /**
     * Suscribe un usuario a un plan Premium.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción creada
     * @throws IllegalArgumentException si el usuario no existe o ya tiene suscripción activa
     */
    @Transactional
    public Suscripcion suscribirse(Long usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar si ya tiene una suscripción activa
        Optional<Suscripcion> suscripcionActiva = obtenerMiSuscripcion(usuarioId);
        if (suscripcionActiva.isPresent()) {
            throw new IllegalArgumentException("Ya tienes una suscripción activa");
        }
        Suscripcion suscripcion = Suscripcion.suscribir();
        suscripcion.setUsuario(usuario);

        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        return guardada;
    }

    /**
     * Cancela la suscripción de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción cancelada
     * @throws IllegalArgumentException si no tiene suscripción activa
     */
    @Transactional
    public Suscripcion cancelarSuscripcion(Long usuarioId) {
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioIdAndActiva(usuarioId, true)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No tienes una suscripción activa"));

        suscripcion.cancelar();
        Suscripcion cancelada = suscripcionRepository.save(suscripcion);
        return cancelada;
    }

    /**
     * Renueva la suscripción de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción renovada
     * @throws IllegalArgumentException si no tiene suscripción
     */
    @Transactional
    public Suscripcion renovarSuscripcion(Long usuarioId) {
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El usuario no tiene suscripción"));

        suscripcion.renovar();
        Suscripcion renovada = suscripcionRepository.save(suscripcion);
        return renovada;
    }
}
