package es.us.meerkat.backend.repository.communities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class MiembroComunidadRepositoryTest {

    @Autowired private MiembroComunidadRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario creador;
    private Usuario miembro1;
    private Usuario miembro2;
    private Comunidad comunidad;

    @BeforeEach
    void setUp() {
        creador = Usuario.builder().email("creador@test.com").password("pass").build();
        em.persist(creador);

        miembro1 = Usuario.builder().email("miembro1@test.com").password("pass").build();
        em.persist(miembro1);

        miembro2 = Usuario.builder().email("miembro2@test.com").password("pass").build();
        em.persist(miembro2);

        comunidad = Comunidad.builder().nombre("Test Community").creador(creador).build();
        em.persist(comunidad);

        em.flush();
    }

    private MiembroComunidad buildMiembro(
            Usuario user, RolComunidad rol, LocalDateTime fechaIngreso) {
        return MiembroComunidad.builder()
                .usuario(user)
                .comunidad(comunidad)
                .rol(rol)
                .fechaIngreso(fechaIngreso)
                .build();
    }

    @Test
    void findByUsuarioIdAndComunidadId_findsMember() {
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        Optional<MiembroComunidad> result =
                repository.findByUsuarioIdAndComunidadId(miembro1.getId(), comunidad.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRol()).isEqualTo(RolComunidad.ALUMNO);
    }

    @Test
    void findByUsuarioIdAndComunidadId_notMember_returnsEmpty() {
        em.flush();

        Optional<MiembroComunidad> result =
                repository.findByUsuarioIdAndComunidadId(miembro1.getId(), comunidad.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void countByComunidadId_countsCorrectly() {
        em.persist(buildMiembro(creador, RolComunidad.ADMIN, LocalDateTime.now().minusDays(10)));
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now().minusDays(5)));
        em.persist(buildMiembro(miembro2, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        long count = repository.countByComunidadId(comunidad.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByComunidadIdAndRol_countsByRole() {
        em.persist(buildMiembro(creador, RolComunidad.ADMIN, LocalDateTime.now()));
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.persist(buildMiembro(miembro2, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        long admins = repository.countByComunidadIdAndRol(comunidad.getId(), RolComunidad.ADMIN);
        long alumnos = repository.countByComunidadIdAndRol(comunidad.getId(), RolComunidad.ALUMNO);

        assertThat(admins).isEqualTo(1);
        assertThat(alumnos).isEqualTo(2);
    }

    @Test
    void findMiembrosMasAntiguosEnComunidad_excludesCreatorOrderedByIngreso() {
        em.persist(buildMiembro(creador, RolComunidad.ADMIN, LocalDateTime.now().minusDays(30)));
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now().minusDays(20)));
        em.persist(buildMiembro(miembro2, RolComunidad.ALUMNO, LocalDateTime.now().minusDays(10)));
        em.flush();

        List<Usuario> result =
                repository.findMiembrosMasAntiguosEnComunidad(comunidad.getId(), creador.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(miembro1.getId());
        assertThat(result.get(1).getId()).isEqualTo(miembro2.getId());
    }

    @Test
    void findUsuarioIdsByComunidadId_returnsAllMemberIds() {
        em.persist(buildMiembro(creador, RolComunidad.ADMIN, LocalDateTime.now()));
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        List<Long> ids = repository.findUsuarioIdsByComunidadId(comunidad.getId());

        assertThat(ids).containsExactlyInAnyOrder(creador.getId(), miembro1.getId());
    }

    @Test
    void findByComunidadIdAndRol_filtersByRole() {
        em.persist(buildMiembro(creador, RolComunidad.ADMIN, LocalDateTime.now()));
        em.persist(buildMiembro(miembro1, RolComunidad.PROFESOR, LocalDateTime.now()));
        em.persist(buildMiembro(miembro2, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        List<MiembroComunidad> profesores =
                repository.findByComunidadIdAndRol(comunidad.getId(), RolComunidad.PROFESOR);

        assertThat(profesores).hasSize(1);
        assertThat(profesores.get(0).getUsuario().getId()).isEqualTo(miembro1.getId());
    }

    @Test
    void existsByUsuarioAndComunidad_returnsTrueWhenMember() {
        em.persist(buildMiembro(miembro1, RolComunidad.ALUMNO, LocalDateTime.now()));
        em.flush();

        assertThat(repository.existsByUsuarioAndComunidad(miembro1, comunidad)).isTrue();
        assertThat(repository.existsByUsuarioAndComunidad(miembro2, comunidad)).isFalse();
    }
}
