package es.us.meerkat.backend.repository.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class EventoRepositoryTest {

    @Autowired private EventoRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario creador;
    private Comunidad comunidad;

    @BeforeEach
    void setUp() {
        creador = Usuario.builder().email("creador@test.com").password("pass").build();
        em.persist(creador);

        comunidad = Comunidad.builder().nombre("Test Community").creador(creador).build();
        em.persist(comunidad);

        em.flush();
    }

    private Evento buildEvento(
            LocalDateTime fechaHora, boolean privado, boolean cancelado, boolean visibleMapa) {
        Evento e = new Evento();
        e.setCreador(creador);
        e.setTitulo("Test Event");
        e.setFechaHora(fechaHora);
        e.setPrivado(privado);
        e.setCancelado(cancelado);
        e.setVisibleMapa(visibleMapa);
        return e;
    }

    @Test
    void findVisibleOnMap_returnsFuturePublicVisibleUncancelled() {
        em.persist(buildEvento(LocalDateTime.now().plusDays(1), false, false, true));
        em.persist(buildEvento(LocalDateTime.now().plusDays(2), false, false, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(3), true, false, true));
        em.persist(buildEvento(LocalDateTime.now().plusDays(4), false, true, true));
        em.persist(buildEvento(LocalDateTime.now().minusDays(1), false, false, true));
        em.flush();

        List<Evento> result = repository.findVisibleOnMap(LocalDateTime.now(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVisibleMapa()).isTrue();
        assertThat(result.get(0).getPrivado()).isFalse();
        assertThat(result.get(0).getCancelado()).isFalse();
    }

    @Test
    void findVisibleOnMap_includesPrivateEventsForCommunityMembers() {
        Evento publico = buildEvento(LocalDateTime.now().plusDays(1), false, false, true);
        publico.setComunidad(comunidad);
        em.persist(publico);

        Evento privadoVisible = buildEvento(LocalDateTime.now().plusDays(2), true, false, true);
        privadoVisible.setComunidad(comunidad);
        em.persist(privadoVisible);

        em.persist(MiembroComunidad.builder().usuario(creador).comunidad(comunidad).build());
        em.flush();

        List<Evento> asMember = repository.findVisibleOnMap(LocalDateTime.now(), creador.getId());
        assertThat(asMember).hasSize(2);

        List<Evento> asAnonymous = repository.findVisibleOnMap(LocalDateTime.now(), null);
        assertThat(asAnonymous).hasSize(1);
        assertThat(asAnonymous.get(0).getPrivado()).isFalse();
    }

    @Test
    void findVisibleOnMap_excludesPrivateEventsForNonMembers() {
        Evento privadoVisible = buildEvento(LocalDateTime.now().plusDays(2), true, false, true);
        privadoVisible.setComunidad(comunidad);
        em.persist(privadoVisible);

        Usuario foraneo = Usuario.builder().email("ajeno@test.com").password("pass").build();
        em.persist(foraneo);
        em.flush();

        List<Evento> result = repository.findVisibleOnMap(LocalDateTime.now(), foraneo.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicEvents_returnsFuturePublicUncancelled() {
        em.persist(buildEvento(LocalDateTime.now().plusDays(1), false, false, true));
        em.persist(buildEvento(LocalDateTime.now().plusDays(2), false, false, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(3), true, false, true));
        em.persist(buildEvento(LocalDateTime.now().minusDays(1), false, false, true));
        em.flush();

        List<Evento> result = repository.findPublicEvents(LocalDateTime.now());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> !e.getPrivado() && !e.getCancelado());
    }

    @Test
    void findPrivateEvents_returnsPrivateUncancelled() {
        em.persist(buildEvento(LocalDateTime.now().plusDays(1), true, false, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(2), false, false, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(3), true, true, false));
        em.flush();

        List<Evento> result = repository.findPrivateEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrivado()).isTrue();
        assertThat(result.get(0).getCancelado()).isFalse();
    }

    @Test
    void findByComunidadIdAndCanceladoFalse_filtersCorrectly() {
        Evento e1 = buildEvento(LocalDateTime.now().plusDays(1), false, false, false);
        e1.setComunidad(comunidad);
        em.persist(e1);

        Evento e2 = buildEvento(LocalDateTime.now().plusDays(2), false, true, false);
        e2.setComunidad(comunidad);
        em.persist(e2);

        em.persist(buildEvento(LocalDateTime.now().plusDays(3), false, false, false));
        em.flush();

        List<Evento> result = repository.findByComunidadIdAndCanceladoFalse(comunidad.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComunidad().getId()).isEqualTo(comunidad.getId());
    }

    @Test
    void findByCanceladoFalse_returnsUncancelled() {
        em.persist(buildEvento(LocalDateTime.now().plusDays(1), false, false, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(2), false, true, false));
        em.persist(buildEvento(LocalDateTime.now().plusDays(3), true, false, false));
        em.flush();

        List<Evento> result = repository.findByCanceladoFalse();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> !e.getCancelado());
    }

    @Test
    void disassociateFromComunidad_setsNull() {
        Evento e = buildEvento(LocalDateTime.now().plusDays(1), false, false, false);
        e.setComunidad(comunidad);
        em.persist(e);
        em.flush();

        repository.disassociateFromComunidad(comunidad.getId());
        em.flush();
        em.clear();

        Evento updated = em.find(Evento.class, e.getId());
        assertThat(updated.getComunidad()).isNull();
    }

    @Test
    void findEventosInRange_returnsUncancelledInRange() {
        LocalDateTime now = LocalDateTime.now();
        em.persist(buildEvento(now.plusHours(1), false, false, false));
        em.persist(buildEvento(now.plusHours(3), false, false, false));
        em.persist(buildEvento(now.plusDays(5), false, false, false));
        em.persist(buildEvento(now.plusHours(2), false, true, false));
        em.flush();

        List<Evento> result = repository.findEventosInRange(now, now.plusHours(4));

        assertThat(result).hasSize(2);
    }
}
