package es.us.meerkat.backend.service.notifications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.EstadoAsistencia;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.notifications.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.events.EventoService;
import es.us.meerkat.backend.service.google.GoogleCalendarService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventoServiceNotificationIntegrationTest {

    @Autowired private EventoService eventoService;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private AsistenciaEventoRepository asistenciaEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ComunidadRepository comunidadRepository;
    @Autowired private PreferenciasNotificacionRepository preferenciasNotificacionRepository;

    @MockitoBean private EmailService emailService;
    @MockitoBean private GoogleCalendarService googleCalendarService;

    @Test
    void editarEventoShouldEnviarCorreoCuandoNotificarCambiosDeEventosEsTrue() {
        Usuario creador = usuarioRepository.save(buildUsuario("creator@meerkat.es", "Creador"));
        Usuario asistente =
                usuarioRepository.save(buildUsuario("attendee@meerkat.es", "Asistente"));

        Comunidad comunidad =
                comunidadRepository.save(
                        Comunidad.builder()
                                .nombre("Comunidad Test")
                                .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                                .creador(creador)
                                .build());

        Evento evento = new Evento();
        evento.setTitulo("Evento original");
        evento.setDescripcion("Desc");
        evento.setFechaHora(LocalDateTime.now().plusDays(3));
        evento.setFechaFin(LocalDateTime.now().plusDays(3).plusHours(2));
        evento.setAforo(20);
        evento.setQueLlevar("Cuaderno");
        evento.setEsVirtual(true); // ✅ virtual
        evento.setPrivado(false);
        evento.setVisibleMapa(true);
        evento.setCreador(creador);
        evento.setComunidad(comunidad);
        evento = eventoRepository.save(evento);
        final Long eventoId = evento.getId();

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setUsuario(asistente);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);
        asistencia.setCreatedAt(LocalDateTime.now());
        asistenciaEventoRepository.save(asistencia);

        PreferenciasNotificacion preferencias = new PreferenciasNotificacion();
        preferencias.setUsuario(asistente);
        preferencias.setEmailsActivados(true);
        preferencias.setNotificarCambiosDeEventos(true);
        preferenciasNotificacionRepository.save(preferencias);

        eventoService.editarEvento(
                evento.getId(),
                "Evento actualizado",
                "Desc nueva",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(4).plusHours(2),
                30,
                "Portátil",
                true, // ✅ virtual
                false,
                null, // ✅ sin ubicación
                true);

        verify(emailService)
                .sendEventUpdatedEmail(
                        argThat(u -> u != null && asistente.getId().equals(u.getId())),
                        argThat(e -> e != null && eventoId.equals(e.getId())));
    }

    @Test
    void editarEventoShouldNoEnviarCorreoCuandoNotificarCambiosDeEventosEsFalse() {
        Usuario creador = usuarioRepository.save(buildUsuario("creator2@meerkat.es", "Creador 2"));
        Usuario asistente =
                usuarioRepository.save(buildUsuario("attendee2@meerkat.es", "Asistente 2"));

        Comunidad comunidad =
                comunidadRepository.save(
                        Comunidad.builder()
                                .nombre("Comunidad Test 2")
                                .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                                .creador(creador)
                                .build());

        Evento evento = new Evento();
        evento.setTitulo("Evento original");
        evento.setDescripcion("Desc");
        evento.setFechaHora(LocalDateTime.now().plusDays(3));
        evento.setFechaFin(LocalDateTime.now().plusDays(3).plusHours(2));
        evento.setAforo(20);
        evento.setQueLlevar("Cuaderno");
        evento.setEsVirtual(true); // ✅
        evento.setPrivado(false);
        evento.setVisibleMapa(true);
        evento.setCreador(creador);
        evento.setComunidad(comunidad);
        evento = eventoRepository.save(evento);

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setUsuario(asistente);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);
        asistencia.setCreatedAt(LocalDateTime.now());
        asistenciaEventoRepository.save(asistencia);

        PreferenciasNotificacion preferencias = new PreferenciasNotificacion();
        preferencias.setUsuario(asistente);
        preferencias.setEmailsActivados(true);
        preferencias.setNotificarCambiosDeEventos(false); // ❌
        preferenciasNotificacionRepository.save(preferencias);

        eventoService.editarEvento(
                evento.getId(),
                "Evento actualizado",
                "Desc nueva",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(4).plusHours(2),
                30,
                "Portátil",
                true,
                false,
                null,
                true);

        verify(emailService, never()).sendEventUpdatedEmail(any(Usuario.class), any(Evento.class));
    }

    @Test
    void editarEventoShouldNoEnviarCorreoCuandoEmailsActivadosEsFalse() {
        Usuario creador = usuarioRepository.save(buildUsuario("creator3@meerkat.es", "Creador 3"));
        Usuario asistente =
                usuarioRepository.save(buildUsuario("attendee3@meerkat.es", "Asistente 3"));

        Comunidad comunidad =
                comunidadRepository.save(
                        Comunidad.builder()
                                .nombre("Comunidad Test 3")
                                .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                                .creador(creador)
                                .build());

        Evento evento = new Evento();
        evento.setTitulo("Evento original");
        evento.setDescripcion("Desc");
        evento.setFechaHora(LocalDateTime.now().plusDays(3));
        evento.setFechaFin(LocalDateTime.now().plusDays(3).plusHours(2));
        evento.setAforo(20);
        evento.setQueLlevar("Cuaderno");
        evento.setEsVirtual(true); // ✅
        evento.setPrivado(false);
        evento.setVisibleMapa(true);
        evento.setCreador(creador);
        evento.setComunidad(comunidad);
        evento = eventoRepository.save(evento);

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setUsuario(asistente);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);
        asistencia.setCreatedAt(LocalDateTime.now());
        asistenciaEventoRepository.save(asistencia);

        PreferenciasNotificacion preferencias = new PreferenciasNotificacion();
        preferencias.setUsuario(asistente);
        preferencias.setEmailsActivados(false); // ❌
        preferencias.setNotificarCambiosDeEventos(true);
        preferenciasNotificacionRepository.save(preferencias);

        eventoService.editarEvento(
                evento.getId(),
                "Evento actualizado",
                "Desc nueva",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(4).plusHours(2),
                30,
                "Portátil",
                true,
                false,
                null,
                true);

        verify(emailService, never()).sendEventUpdatedEmail(any(Usuario.class), any(Evento.class));
    }

    private Usuario buildUsuario(final String email, final String nombre) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword("encoded-password");
        usuario.setNombre(nombre);
        usuario.setNotificacionesPush(false);
        usuario.setVisibleEnListados(true);
        return usuario;
    }
}
