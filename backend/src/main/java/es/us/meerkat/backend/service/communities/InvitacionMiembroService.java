package es.us.meerkat.backend.service.communities;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.communities.CreateInvitacionRequest;
import es.us.meerkat.backend.dto.communities.InvitacionResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoInvitacion;
import es.us.meerkat.backend.entity.InvitacionMiembro;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.InvitacionMiembroRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de invitaciones a miembros en comunidades. */
@Service
@RequiredArgsConstructor
@Transactional
public class InvitacionMiembroService {

    private final InvitacionMiembroRepository invitacionRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final MiembroComunidadRepository miembroCommunityRepository;
    private final AuthorizationService authorizationService;
    private final EmailService emailService;

    /**
     * Crea una nueva invitación para un miembro en una comunidad.
     *
     * @param userId ID del usuario administrador
     * @param communityId ID de la comunidad
     * @param request datos de la invitación
     * @return la invitación creada
     * @throws IllegalArgumentException si el usuario no tiene permisos o el email ya existe
     */
    public InvitacionMiembro createInvitacion(
            Long userId, Long communityId, CreateInvitacionRequest request) {
        // Verificar que el usuario es admin de la comunidad
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException(
                    "Solo administradores pueden invitar miembros a esta comunidad");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        Usuario usuarioInvitador =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar que el email no ya tenga una invitación pendiente
        if (invitacionRepository.existsByEmailAndComunidadAndEstado(
                request.email(), comunidad, EstadoInvitacion.PENDIENTE)) {
            throw new IllegalArgumentException(
                    "Este email ya tiene una invitación pendiente en esta comunidad");
        }

        // Verificar que el email no sea ya miembro
        Usuario usuarioExistente = usuarioRepository.findByEmail(request.email()).orElse(null);
        if (usuarioExistente != null
                && miembroCommunityRepository.existsByUsuarioAndComunidad(
                        usuarioExistente, comunidad)) {
            throw new IllegalArgumentException("Este usuario ya es miembro de la comunidad");
        }

        String codigo = UUID.randomUUID().toString();

        InvitacionMiembro invitacion =
                InvitacionMiembro.builder()
                        .email(request.email())
                        .codigo(codigo)
                        .comunidad(comunidad)
                        .usuarioInvitador(usuarioInvitador)
                        .rol(request.rol())
                        .estado(EstadoInvitacion.PENDIENTE)
                        .build();

        InvitacionMiembro savedInvitacion = invitacionRepository.save(invitacion);

        // Enviar email de invitación
        enviarEmailInvitacion(savedInvitacion);

        return savedInvitacion;
    }

    /**
     * Obtiene las invitaciones de una comunidad de forma paginada.
     *
     * @param userId ID del usuario administrador
     * @param communityId ID de la comunidad
     * @param pageable paginación
     * @return página de invitaciones
     * @throws IllegalArgumentException si el usuario no tiene permisos
     */
    @Transactional(readOnly = true)
    public Page<InvitacionMiembro> getInvitacionesByCommunity(
            Long userId, Long communityId, Pageable pageable) {
        // Verificar que el usuario es admin de la comunidad
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo administradores pueden ver las invitaciones");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        return invitacionRepository.findByComunidad(comunidad, pageable);
    }

    /**
     * Obtiene una invitación por su código.
     *
     * @param codigo código de la invitación
     * @return la invitación
     * @throws IllegalArgumentException si no existe
     */
    @Transactional(readOnly = true)
    public InvitacionMiembro getInvitacionByCodigo(String codigo) {
        return invitacionRepository
                .findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));
    }

    /**
     * Acepta una invitación y agrega al usuario a la comunidad.
     *
     * @param codigo código de la invitación
     * @param userId ID del usuario que acepta (puede ser null si es nuevo usuario)
     * @return la invitación actualizada
     * @throws IllegalArgumentException si la invitación es inválida o ha expirado
     */
    public InvitacionMiembro aceptarInvitacion(String codigo, Long userId) {
        InvitacionMiembro invitacion = getInvitacionByCodigo(codigo);

        // Verificar que no ha expirado
        if (invitacion.estaExpirada()) {
            invitacion.setEstado(EstadoInvitacion.EXPIRADA);
            invitacionRepository.save(invitacion);
            throw new IllegalArgumentException("La invitación ha expirado");
        }

        // Verificar que está pendiente
        if (invitacion.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new IllegalArgumentException("Esta invitación ya fue procesada");
        }

        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar que el email coincide (o crear una excepción)
        if (!usuario.getEmail().equalsIgnoreCase(invitacion.getEmail())) {
            throw new IllegalArgumentException(
                    "El email del usuario no coincide con la invitación");
        }

        // Marcar como aceptada
        invitacion.aceptar(usuario);

        // Agregar al usuario como miembro de la comunidad
        MiembroComunidad miembro =
                MiembroComunidad.builder()
                        .usuario(usuario)
                        .comunidad(invitacion.getComunidad())
                        .rol(invitacion.getRol())
                        .build();

        miembroCommunityRepository.save(miembro);

        return invitacionRepository.save(invitacion);
    }

    /**
     * Rechaza una invitación.
     *
     * @param codigo código de la invitación
     * @return la invitación actualizada
     * @throws IllegalArgumentException si la invitación no está pendiente
     */
    public InvitacionMiembro rechazarInvitacion(String codigo) {
        InvitacionMiembro invitacion = getInvitacionByCodigo(codigo);

        if (invitacion.getEstado() != EstadoInvitacion.PENDIENTE) {
            throw new IllegalArgumentException("Esta invitación ya fue procesada");
        }

        invitacion.setEstado(EstadoInvitacion.RECHAZADA);
        return invitacionRepository.save(invitacion);
    }

    /**
     * Convierte una invitación a su DTO de respuesta.
     *
     * @param invitacion la invitación
     * @return el DTO
     */
    public InvitacionResponse toResponse(InvitacionMiembro invitacion) {
        return new InvitacionResponse(
                invitacion.getId(),
                invitacion.getEmail(),
                invitacion.getCodigo(),
                invitacion.getRol(),
                invitacion.getEstado(),
                invitacion.getCreatedAt(),
                invitacion.getFechaExpiracion(),
                invitacion.getFechaAceptacion(),
                new es.us.meerkat.backend.dto.users.UserSimpleResponse(
                        invitacion.getUsuarioInvitador().getId(),
                        invitacion.getUsuarioInvitador().getNombre(),
                        invitacion.getUsuarioInvitador().getEmail(),
                        invitacion.getUsuarioInvitador().getFoto()),
                invitacion.getUsuarioAceptador() != null
                        ? new es.us.meerkat.backend.dto.users.UserSimpleResponse(
                                invitacion.getUsuarioAceptador().getId(),
                                invitacion.getUsuarioAceptador().getNombre(),
                                invitacion.getUsuarioAceptador().getEmail(),
                                invitacion.getUsuarioAceptador().getFoto())
                        : null);
    }

    /**
     * Envía un email de invitación al destinatario.
     *
     * @param invitacion la invitación
     */
    private void enviarEmailInvitacion(InvitacionMiembro invitacion) {
        try {
            String enlaceAceptacion =
                    "http://localhost:3000/invitacion/" + invitacion.getCodigo() + "/aceptar";
            String subject = "Invitación a la comunidad: " + invitacion.getComunidad().getNombre();
            String mensaje =
                    String.format(
                            "Has sido invitado a unirte a la comunidad '%s'.%n%n"
                                    + "Haz clic en el siguiente enlace para aceptar:%n%s%n%n"
                                    + "Esta invitación expira en 30 días.",
                            invitacion.getComunidad().getNombre(), enlaceAceptacion);

            emailService.sendSimpleEmail(invitacion.getEmail(), subject, mensaje);
        } catch (Exception e) {
            // Log the error but don't fail the operation
            System.err.println("Error sending invitation email: " + e.getMessage());
        }
    }
}
