package es.us.meerkat.backend.repository.recommendations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import es.us.meerkat.backend.entity.recommendations.Recomendacion;
import es.us.meerkat.backend.entity.recommendations.TipoRecomendacion;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class RecomendacionRepositoryTest {

    @Autowired private RecomendacionRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().email("user@test.com").password("pass").build();
        em.persist(usuario);
        em.flush();
    }

    private Recomendacion buildRec(
            TipoRecomendacion tipo, Double puntuacion, boolean vista, LocalDateTime expiracion) {
        return Recomendacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .idObjetoRecomendado(1L)
                .titulo("Test")
                .puntuacionRelevancia(puntuacion)
                .vista(vista)
                .fechaExpiracion(expiracion)
                .build();
    }

    @Test
    void findPorTipo_returnsMatchingTypeNotExpired() {
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.9, false, null));
        em.persist(buildRec(TipoRecomendacion.EVENTO, 0.8, false, null));
        em.persist(
                buildRec(TipoRecomendacion.PROFESOR, 0.7, false, LocalDateTime.now().minusDays(1)));
        em.flush();

        Page<Recomendacion> result =
                repository.findPorTipo(
                        usuario.getId(), TipoRecomendacion.PROFESOR, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTipo()).isEqualTo(TipoRecomendacion.PROFESOR);
    }

    @Test
    void findRecomendacionesActivas_returnsAllNotExpiredOrderedByPuntuacion() {
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.5, false, null));
        em.persist(buildRec(TipoRecomendacion.EVENTO, 0.9, false, LocalDateTime.now().plusDays(7)));
        em.persist(
                buildRec(
                        TipoRecomendacion.CONTENIDO, 0.7, false, LocalDateTime.now().minusDays(1)));
        em.flush();

        Page<Recomendacion> result =
                repository.findRecomendacionesActivas(usuario.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPuntuacionRelevancia())
                .isGreaterThanOrEqualTo(result.getContent().get(1).getPuntuacionRelevancia());
    }

    @Test
    void findByUsuarioTipoObjeto_findsExactMatch() {
        Recomendacion r = buildRec(TipoRecomendacion.COMUNIDAD, 0.5, false, null);
        r.setIdObjetoRecomendado(42L);
        em.persist(r);
        em.flush();

        List<Recomendacion> result =
                repository.findByUsuarioTipoObjeto(
                        usuario.getId(), TipoRecomendacion.COMUNIDAD, 42L);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByUsuarioTipoObjeto_noMatch_returnsEmpty() {
        em.persist(buildRec(TipoRecomendacion.COMUNIDAD, 0.5, false, null));
        em.flush();

        List<Recomendacion> result =
                repository.findByUsuarioTipoObjeto(
                        usuario.getId(), TipoRecomendacion.PROFESOR, 999L);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteExpiradas_removesExpiredOnly() {
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.9, false, null));
        em.persist(
                buildRec(TipoRecomendacion.PROFESOR, 0.5, false, LocalDateTime.now().minusDays(1)));
        em.persist(buildRec(TipoRecomendacion.EVENTO, 0.5, false, LocalDateTime.now().plusDays(7)));
        em.flush();

        repository.deleteExpiradas(usuario.getId(), LocalDateTime.now());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void deleteByUsuarioIdAndTipo_removesAllOfType() {
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.9, false, null));
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.5, false, null));
        em.persist(buildRec(TipoRecomendacion.EVENTO, 0.8, false, null));
        em.flush();

        repository.deleteByUsuarioIdAndTipo(usuario.getId(), TipoRecomendacion.PROFESOR);
        em.flush();
        em.clear();

        List<Recomendacion> remaining = repository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getTipo()).isEqualTo(TipoRecomendacion.EVENTO);
    }

    @Test
    void findNoVistas_returnsUnseenNotExpired() {
        em.persist(buildRec(TipoRecomendacion.PROFESOR, 0.9, false, null));
        em.persist(buildRec(TipoRecomendacion.EVENTO, 0.8, true, null));
        em.persist(
                buildRec(
                        TipoRecomendacion.CONTENIDO, 0.7, false, LocalDateTime.now().minusDays(1)));
        em.flush();

        Page<Recomendacion> result =
                repository.findNoVistas(usuario.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getVista()).isFalse();
    }

    @Test
    void countByUsuarioIdAndEsFavorable_countsCorrectly() {
        Recomendacion r1 = buildRec(TipoRecomendacion.PROFESOR, 0.9, false, null);
        r1.setEsFavorable(true);
        em.persist(r1);
        Recomendacion r2 = buildRec(TipoRecomendacion.EVENTO, 0.8, false, null);
        r2.setEsFavorable(true);
        em.persist(r2);
        Recomendacion r3 = buildRec(TipoRecomendacion.CONTENIDO, 0.7, false, null);
        r3.setEsFavorable(false);
        em.persist(r3);
        em.flush();

        long favorable = repository.countByUsuarioIdAndEsFavorable(usuario.getId(), true);
        long desfavorable = repository.countByUsuarioIdAndEsFavorable(usuario.getId(), false);

        assertThat(favorable).isEqualTo(2);
        assertThat(desfavorable).isEqualTo(1);
    }
}
