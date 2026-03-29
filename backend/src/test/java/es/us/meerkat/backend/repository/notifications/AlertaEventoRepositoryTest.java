package es.us.meerkat.backend.repository.notifications;

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

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.notifications.AlertaEvento;
import es.us.meerkat.backend.entity.notifications.TipoAlerta;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class AlertaEventoRepositoryTest {

    @Autowired private AlertaEventoRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().email("user@test.com").password("pass").build();
        em.persist(usuario);

        evento = new Evento();
        evento.setCreador(usuario);
        evento.setTitulo("Test Event");
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setCancelado(false);
        evento.setPrivado(false);
        em.persist(evento);

        em.flush();
    }

    private AlertaEvento buildAlerta(TipoAlerta tipo, boolean leida) {
        AlertaEvento alerta = new AlertaEvento();
        alerta.setEvento(evento);
        alerta.setUsuario(usuario);
        alerta.setTipo(tipo);
        alerta.setMensaje("Alerta test");
        alerta.setLeida(leida);
        if (leida) {
            alerta.setLeidaAt(LocalDateTime.now().minusHours(1));
        }
        return alerta;
    }

    @Test
    void findUnreadByUsuarioId_returnsUnreadOnly() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, false));
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, true));
        em.flush();

        List<AlertaEvento> result = repository.findUnreadByUsuarioId(usuario.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> !a.getLeida());
    }

    @Test
    void findAllByUsuarioId_returnsAll() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, true));
        em.flush();

        List<AlertaEvento> result = repository.findAllByUsuarioId(usuario.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void countUnreadByUsuarioId_countsCorrectly() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, false));
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, true));
        em.flush();

        Long count = repository.countUnreadByUsuarioId(usuario.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void markAllAsReadByUsuarioId_updatesAllUnread() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, false));
        em.flush();

        LocalDateTime readAt = LocalDateTime.now();
        repository.markAllAsReadByUsuarioId(usuario.getId(), readAt);
        em.flush();
        em.clear();

        List<AlertaEvento> all = repository.findAll();
        assertThat(all).allMatch(AlertaEvento::getLeida);
    }

    @Test
    void findEventoIdsWithAlertaInRange_returnsDistinctIds() {
        LocalDateTime eventTime = evento.getFechaHora();
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));

        Evento evento2 = new Evento();
        evento2.setCreador(usuario);
        evento2.setTitulo("Event 2");
        evento2.setFechaHora(eventTime.plusHours(2));
        evento2.setCancelado(false);
        evento2.setPrivado(false);
        em.persist(evento2);
        AlertaEvento alerta2 = new AlertaEvento();
        alerta2.setEvento(evento2);
        alerta2.setUsuario(usuario);
        alerta2.setTipo(TipoAlerta.PROXIMA_24H);
        alerta2.setMensaje("Alerta 2");
        alerta2.setLeida(false);
        em.persist(alerta2);
        em.flush();

        List<Long> ids =
                repository.findEventoIdsWithAlertaInRange(
                        eventTime.minusHours(1), eventTime.plusHours(3), TipoAlerta.PROXIMA_24H);

        assertThat(ids).hasSize(2);
    }

    @Test
    void findByEventoIdAndUsuarioIdAndTipo_findsExisting() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.flush();

        Optional<AlertaEvento> result =
                repository.findByEventoIdAndUsuarioIdAndTipo(
                        evento.getId(), usuario.getId(), TipoAlerta.PROXIMA_24H);

        assertThat(result).isPresent();
    }

    @Test
    void findByEventoIdAndUsuarioIdAndTipo_notFound() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.flush();

        Optional<AlertaEvento> result =
                repository.findByEventoIdAndUsuarioIdAndTipo(
                        evento.getId(), usuario.getId(), TipoAlerta.INMINENTE_15MIN);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteOldReadAlertas_removesOldRead() {
        AlertaEvento old = buildAlerta(TipoAlerta.PROXIMA_24H, true);
        old.setLeidaAt(LocalDateTime.now().minusDays(30));
        em.persist(old);
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, false));
        em.flush();

        repository.deleteOldReadAlertas(LocalDateTime.now().minusDays(7));
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void deleteByEventoId_removesAllForEvent() {
        em.persist(buildAlerta(TipoAlerta.PROXIMA_24H, false));
        em.persist(buildAlerta(TipoAlerta.INMINENTE_15MIN, false));
        em.flush();

        repository.deleteByEventoId(evento.getId());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).isEmpty();
    }
}
