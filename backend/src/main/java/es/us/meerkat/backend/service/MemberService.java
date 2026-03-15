package es.us.meerkat.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.ComunidadClassroom;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MiembroComunidadRepository miembroComunidadRepository;
    private final ComunidadRepository comunidadRepository;
    private final ComunidadClassroomRepository comunidadClassroomRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final CommunityService communityService;
    private final GoogleClassroomService googleClassroomService;

    /** Se une a una comunidad pública (verifica aforo y tipo). */
    public MiembroComunidad joinPublicCommunity(Long userId, Long communityId) {
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Validar que sea pública
        if (comunidad.getTipoGrupo() == TipoGrupo.GRUPO_PRIVADO) {
            throw new IllegalArgumentException(
                    "No puedes unirte a una comunidad privada directamente. Solicita acceso.");
        }

        // Validar que no sea miembro ya
        if (authorizationService.isMemberOf(userId, communityId)) {
            throw new IllegalArgumentException("Ya eres miembro de esta comunidad");
        }

        // Validar aforo
        long currentMembers = communityService.countMembers(communityId);
        int maxMembers = communityService.getMaxMembers(communityId);
        if (currentMembers >= maxMembers) {
            throw new IllegalArgumentException("La comunidad está llena");
        }

        // Crear membresía
        MiembroComunidad miembro =
                MiembroComunidad.builder()
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .rol(RolComunidad.ALUMNO)
                        .build();

        MiembroComunidad miembroGuardado = miembroComunidadRepository.save(miembro);

        // Sincronizar con Google Classroom si existe vinculación
        sincronizarConClassroom(usuario, comunidad);

        return miembroGuardado;
    }

    /** Abandona una comunidad. Si es único admin, lanza error. */
    public void leaveCommunity(Long userId, Long communityId) {
        MiembroComunidad miembro =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(userId, communityId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No eres miembro de esta comunidad"));

        // Si es ADMIN, verificar que haya otros ADMINs
        if (miembro.getRol() == RolComunidad.ADMIN) {
            long adminCount =
                    miembroComunidadRepository.countByComunidadIdAndRol(
                            communityId, RolComunidad.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException(
                        "No puedes abandonar siendo el único admin. Transfiere la administración"
                                + " primero.");
            }
        }

        Usuario usuario = miembro.getUsuario();
        Comunidad comunidad = miembro.getComunidad();

        // Desincronizar de Google Classroom
        desincronizarDeClassroom(usuario, comunidad);

        miembroComunidadRepository.delete(miembro);
    }

    /** Obtiene la membresía de un usuario en una comunidad. */
    @Transactional(readOnly = true)
    public MiembroComunidad getMyMembership(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .orElseThrow(
                        () -> new IllegalArgumentException("No eres miembro de esta comunidad"));
    }

    /** Expulsa a un miembro (solo ADMIN). */
    public void expelMember(Long userId, Long communityId, Long targetUserId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden expulsar miembros");
        }

        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo");
        }

        MiembroComunidad targetMiembro =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(targetUserId, communityId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El usuario no es miembro de esta comunidad"));

        // No se puede expulsar al único admin
        if (targetMiembro.getRol() == RolComunidad.ADMIN) {
            long adminCount =
                    miembroComunidadRepository.countByComunidadIdAndRol(
                            communityId, RolComunidad.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("No puedes expulsar al único admin");
            }
        }

        Usuario targetUsuario = targetMiembro.getUsuario();
        Comunidad comunidad = targetMiembro.getComunidad();

        // Desincronizar de Google Classroom
        desincronizarDeClassroom(targetUsuario, comunidad);

        miembroComunidadRepository.delete(targetMiembro);
    }

    /** Lista los miembros de una comunidad. */
    @Transactional(readOnly = true)
    public Page<MiembroComunidad> listMembers(Long communityId, Pageable pageable) {
        return miembroComunidadRepository.findByComunidadId(communityId, pageable);
    }

    /** Lista las membresías de comunidades de un usuario. */
    @Transactional(readOnly = true)
    public Page<MiembroComunidad> listUserMemberships(Long userId, Pageable pageable) {
        return miembroComunidadRepository.findByUsuarioId(userId, pageable);
    }

    /** Transfiere el rol ADMIN a otro miembro (solo ADMIN actual). */
    public MiembroComunidad transferAdmin(Long userId, Long communityId, Long newAdminId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden transferir administración");
        }

        if (userId.equals(newAdminId)) {
            throw new IllegalArgumentException("No puedes transferir a ti mismo");
        }

        // Verificar que el nuevo admin sea miembro
        MiembroComunidad nuevoAdmin =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(newAdminId, communityId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El nuevo admin debe ser miembro de la comunidad"));

        MiembroComunidad usuarioActual =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(userId, communityId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No eres miembro de esta comunidad"));

        // Cambiar roles
        usuarioActual.setRol(RolComunidad.ALUMNO);
        nuevoAdmin.setRol(RolComunidad.ADMIN);

        miembroComunidadRepository.save(usuarioActual);
        return miembroComunidadRepository.save(nuevoAdmin);
    }

    /** Cuenta los ADMINs de una comunidad específica. */
    @Transactional(readOnly = true)
    public long countAdmins(Long communityId) {
        return miembroComunidadRepository.countByComunidadIdAndRol(communityId, RolComunidad.ADMIN);
    }

    // =====================================================
    // SINCRONIZACIÓN CON GOOGLE CLASSROOM
    // =====================================================

    /**
     * Sincroniza un usuario con Google Classroom cuando se une a una comunidad. Si la comunidad
     * tiene una vinculación activa con Classroom, agrega automáticamente al usuario como estudiante
     * o profesor según su rol de tutor.
     *
     * @param usuario Usuario que se une a la comunidad
     * @param comunidad Comunidad a la que se une
     */
    private void sincronizarConClassroom(Usuario usuario, Comunidad comunidad) {
        try {
            // Verificar si la comunidad tiene vinculación con Google Classroom
            ComunidadClassroom vinculacion =
                    comunidadClassroomRepository.findByComunidadId(comunidad.getId()).orElse(null);

            if (vinculacion == null || !vinculacion.getActiva()) {
                return;
            }

            // Obtener el ADMIN de la comunidad para obtener su token
            Usuario adminComunidad =
                    miembroComunidadRepository
                            .findByComunidadIdAndRol(comunidad.getId(), RolComunidad.ADMIN)
                            .stream()
                            .findFirst()
                            .map(MiembroComunidad::getUsuario)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "No se encontró admin de la comunidad para"
                                                            + " sincronizar con Classroom"));

            // Determinar el rol en Classroom basado en si es tutor
            if (usuario.getEsTutor() != null && usuario.getEsTutor()) {
                // Agregar como TEACHER
                agregarProfesorAClassroom(
                        adminComunidad, usuario, vinculacion.getClassroomCourseId());
            } else {
                // Agregar como STUDENT
                agregarEstudianteAClassroom(
                        adminComunidad, usuario, vinculacion.getClassroomCourseId());
            }

        } catch (Exception e) {
            System.out.println("Jeje god" + e);
        }
    }

    /**
     * Agrega un usuario como estudiante a un curso de Google Classroom.
     *
     * @param adminUsuario Usuario admin con token OAuth
     * @param studiantUsuario Usuario a agregar como estudiante
     * @param courseId ID del curso en Google Classroom
     */
    private void agregarEstudianteAClassroom(
            Usuario adminUsuario, Usuario studiantUsuario, String courseId) {
        String studentData = String.format("{\"userId\":\"%s\"}", studiantUsuario.getEmail());
        googleClassroomService.crearEstudiante(adminUsuario, courseId, studentData);
    }

    /**
     * Agrega un usuario como profesor a un curso de Google Classroom.
     *
     * @param adminUsuario Usuario admin con token OAuth
     * @param teacherUsuario Usuario a agregar como profesor
     * @param courseId ID del curso en Google Classroom
     */
    private void agregarProfesorAClassroom(
            Usuario adminUsuario, Usuario teacherUsuario, String courseId) {
        String teacherData = String.format("{\"userId\":\"%s\"}", teacherUsuario.getEmail());
        googleClassroomService.crearProfesor(adminUsuario, courseId, teacherData);
    }

    /**
     * Desincroniza un usuario de Google Classroom cuando abandona o es expulsado de una comunidad.
     * Si la comunidad tiene una vinculación activa con Classroom, elimina automáticamente al
     * usuario como estudiante o profesor según su rol de tutor.
     *
     * @param usuario Usuario que abandona o es expulsado de la comunidad
     * @param comunidad Comunidad de la que se va
     */
    private void desincronizarDeClassroom(Usuario usuario, Comunidad comunidad) {
        try {
            // Verificar si la comunidad tiene vinculación con Google Classroom
            ComunidadClassroom vinculacion =
                    comunidadClassroomRepository.findByComunidadId(comunidad.getId()).orElse(null);

            if (vinculacion == null || !vinculacion.getActiva()) {
                return;
            }

            // Obtener el ADMIN de la comunidad para obtener su token
            Usuario adminComunidad =
                    miembroComunidadRepository
                            .findByComunidadIdAndRol(comunidad.getId(), RolComunidad.ADMIN)
                            .stream()
                            .findFirst()
                            .map(MiembroComunidad::getUsuario)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "No se encontró admin de la comunidad para"
                                                            + " desincronizar con Classroom"));

            // Determinar el rol en Classroom basado en si es tutor
            if (usuario.getEsTutor() != null && usuario.getEsTutor()) {
                // Eliminar como TEACHER
                eliminarProfesorDeClassroom(
                        adminComunidad, usuario, vinculacion.getClassroomCourseId());
            } else {
                // Eliminar como STUDENT
                eliminarEstudianteDeClassroom(
                        adminComunidad, usuario, vinculacion.getClassroomCourseId());
            }

        } catch (Exception e) {
            System.out.println("Jeje god" + e);
        }
    }

    /**
     * Elimina un usuario como estudiante de un curso de Google Classroom.
     *
     * @param adminUsuario Usuario admin con token OAuth
     * @param studentUsuario Usuario a eliminar como estudiante
     * @param courseId ID del curso en Google Classroom
     */
    private void eliminarEstudianteDeClassroom(
            Usuario adminUsuario, Usuario studentUsuario, String courseId) {
        googleClassroomService.eliminarEstudiante(
                adminUsuario, courseId, studentUsuario.getEmail());
    }

    /**
     * Elimina un usuario como profesor de un curso de Google Classroom.
     *
     * @param adminUsuario Usuario admin con token OAuth
     * @param teacherUsuario Usuario a eliminar como profesor
     * @param courseId ID del curso en Google Classroom
     */
    private void eliminarProfesorDeClassroom(
            Usuario adminUsuario, Usuario teacherUsuario, String courseId) {
        googleClassroomService.eliminarProfesor(adminUsuario, courseId, teacherUsuario.getEmail());
    }
}
