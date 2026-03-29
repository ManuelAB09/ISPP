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
import es.us.meerkat.backend.entity.notifications.AlarmaPersonalizada;
import es.us.meerkat.backend.entity.notifications.TipoCanal;
import es.us.meerkat.backend.entity.users.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class AlarmaPersonalizadaRepositoryTest {

    @Autowired private AlarmaPersonalizadaRepository repository;

    @Autowired private TestEntityManager em;

    private Usuario usuario;
    private Usuario otroUsuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().email("user@test.com").password("pass").build();
        em.persist(usuario);

        otroUsuario = Usuario.builder().email("otro@test.com").password("pass").build();
        em.persist(otroUsuario);

        evento = new Evento();
        evento.setCreador(usuario);
        evento.setTitulo("Test Event");
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setCancelado(false);
        evento.setPrivado(false);
        em.persist(evento);

        em.flush();
    }

    private AlarmaPersonalizada buildAlarma(
            Usuario user,
            Evento ev,
            int minutosAntes,
            boolean disparada,
            LocalDateTime fechaDisparo) {
        AlarmaPersonalizada a = new AlarmaPersonalizada();
        a.setUsuario(user);
        a.setEvento(ev);
        a.setMinutosAntes(minutosAntes);
        a.setCanal(TipoCanal.PLATAFORMA);
        a.setDisparada(disparada);
        a.setFechaDisparo(fechaDisparo);
        if (disparada) {
            a.setDisparadaAt(fechaDisparo);
        }
        return a;
    }

    @Test
    void findPendientesByUsuarioId_returnsUndispatchedOrderedByFechaDisparo() {
        em.persist(buildAlarma(usuario, evento, 30, false, LocalDateTime.now().plusHours(2)));
        em.persist(buildAlarma(usuario, evento, 60, false, LocalDateTime.now().plusHours(1)));
        em.persist(buildAlarma(usuario, evento, 15, true, LocalDateTime.now().minusHours(1)));
        em.flush();

        List<AlarmaPersonalizada> result = repository.findPendientesByUsuarioId(usuario.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFechaDisparo()).isBefore(result.get(1).getFechaDisparo());
    }

    @Test
    void findAlarmasPendientesADisparar_returnsReadyAlarmsOfNonCancelledEvents() {
        // Event that started 10 min ago, not cancelled
        Evento pastEvent = new Evento();
        pastEvent.setCreador(usuario);
        pastEvent.setTitulo("Past");
        pastEvent.setFechaHora(LocalDateTime.now().minusMinutes(10));
        pastEvent.setCancelado(false);
        pastEvent.setPrivado(false);
        em.persistAndFlush(pastEvent);

        // Alarm 5 min before → fechaDisparo = now-10-5 = now-15 (past, ready to fire)
        AlarmaPersonalizada ready = new AlarmaPersonalizada();
        ready.setUsuario(usuario);
        ready.setEvento(pastEvent);
        ready.setMinutosAntes(5);
        ready.setCanal(TipoCanal.PLATAFORMA);
        ready.setDisparada(false);
        em.persistAndFlush(ready);

        // Alarm on future event → fechaDisparo far in future
        AlarmaPersonalizada future = new AlarmaPersonalizada();
        future.setUsuario(usuario);
        future.setEvento(evento); // evento from setUp, fechaHora = now+1day
        future.setMinutosAntes(30);
        future.setCanal(TipoCanal.PLATAFORMA);
        future.setDisparada(false);
        em.persistAndFlush(future);

        em.clear();

        List<AlarmaPersonalizada> result =
                repository.findAlarmasPendientesADisparar(LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEvento().getTitulo()).isEqualTo("Past");
    }

    @Test
    void deleteByEventoIdAndUsuarioId_removesOnlyTargeted() {
        em.persist(buildAlarma(usuario, evento, 30, false, LocalDateTime.now().plusHours(1)));
        em.persist(buildAlarma(otroUsuario, evento, 30, false, LocalDateTime.now().plusHours(1)));
        em.flush();

        repository.deleteByEventoIdAndUsuarioId(evento.getId(), usuario.getId());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getUsuario().getId()).isEqualTo(otroUsuario.getId());
    }

    @Test
    void deleteOldDisparadas_removesOldFiredAlarms() {
        AlarmaPersonalizada old =
                buildAlarma(usuario, evento, 30, true, LocalDateTime.now().minusDays(40));
        old.setDisparadaAt(LocalDateTime.now().minusDays(40));
        em.persist(old);
        em.persist(buildAlarma(usuario, evento, 60, false, LocalDateTime.now().plusHours(1)));
        em.flush();

        repository.deleteOldDisparadas(LocalDateTime.now().minusDays(30));
        em.flush();
        em.clear();

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getDisparada()).isFalse();
    }

    @Test
    void deleteByEventoId_removesAllAlarmsForEvent() {
        em.persist(buildAlarma(usuario, evento, 30, false, LocalDateTime.now().plusHours(1)));
        em.persist(buildAlarma(otroUsuario, evento, 60, false, LocalDateTime.now().plusHours(2)));
        em.flush();

        repository.deleteByEventoId(evento.getId());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findByEventoIdAndUsuarioIdAndMinutosAntes_findsDuplicate() {
        em.persist(buildAlarma(usuario, evento, 30, false, LocalDateTime.now().plusHours(1)));
        em.flush();

        Optional<AlarmaPersonalizada> result =
                repository.findByEventoIdAndUsuarioIdAndMinutosAntes(
                        evento.getId(), usuario.getId(), 30);

        assertThat(result).isPresent();
    }

    @Test
    void findByEventoIdAndUsuarioIdAndMinutosAntes_noDuplicate() {
        em.persist(buildAlarma(usuario, evento, 30, false, LocalDateTime.now().plusHours(1)));
        em.flush();

        Optional<AlarmaPersonalizada> result =
                repository.findByEventoIdAndUsuarioIdAndMinutosAntes(
                        evento.getId(), usuario.getId(), 60);

        assertThat(result).isEmpty();
    }
}
