package es.us.meerkat.backend.service.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.events.UpdatePreferenciasRequest;
import es.us.meerkat.backend.dto.notifications.PreferenciasNotificacionResponse;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.notifications.TipoCanal;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.notifications.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PreferenciasNotificacionServiceTest {

    @Mock private PreferenciasNotificacionRepository preferenciasRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PreferenciasNotificacionService preferenciasService;

    // ================================================================
    // getOrCreate
    // ================================================================

    @Test
    void getOrCreateShouldReturnExistingPreferencesWhenFound() {
        PreferenciasNotificacion existing = new PreferenciasNotificacion();
        existing.setId(1L);
        existing.setEmailsActivados(true);

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.of(existing));

        PreferenciasNotificacion result = preferenciasService.getOrCreate(10L);

        assertThat(result).isEqualTo(existing);
        assertThat(result.getEmailsActivados()).isTrue();
    }

    @Test
    void getOrCreateShouldCreateNewPreferencesWhenNotFound() {
        Usuario usuario =
                Usuario.builder().id(10L).nombre("U").email("u@t.com").password("p").build();

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciasRepository.save(any(PreferenciasNotificacion.class)))
                .thenAnswer(
                        inv -> {
                            PreferenciasNotificacion p = inv.getArgument(0);
                            p.setId(1L);
                            return p;
                        });

        PreferenciasNotificacion result = preferenciasService.getOrCreate(10L);

        assertThat(result.getUsuario()).isEqualTo(usuario);
        verify(preferenciasRepository).save(any(PreferenciasNotificacion.class));
    }

    @Test
    void getOrCreateShouldThrowWhenUserNotFound() {
        when(preferenciasRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preferenciasService.getOrCreate(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ================================================================
    // obtenerPreferencias
    // ================================================================

    @Test
    void obtenerPreferenciasShouldReturnResponseDto() {
        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setId(1L);
        prefs.setEmailsActivados(true);
        prefs.setRecordatorio24h(true);
        prefs.setRecordatorio1h(false);
        prefs.setRecordatorio30min(false);
        prefs.setCanalAlarmasPorDefecto(TipoCanal.AMBOS);
        prefs.setNotificarMensajeComunidad(true);
        prefs.setNotificarMenciones(false);
        prefs.setNotificarInvitaciones(true);
        prefs.setNotificarAnuncios(true);
        prefs.setNotificarSolicitudAcceso(false);
        prefs.setNotificarCambiosDeEventos(true);

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.of(prefs));

        PreferenciasNotificacionResponse response = preferenciasService.obtenerPreferencias(10L);

        assertThat(response.getEmailsActivados()).isTrue();
        assertThat(response.getRecordatorio24h()).isTrue();
        assertThat(response.getRecordatorio1h()).isFalse();
        assertThat(response.getRecordatorio30min()).isFalse();
        assertThat(response.getCanalAlarmasPorDefecto()).isEqualTo(TipoCanal.AMBOS);
        assertThat(response.getNotificarMensajeComunidad()).isTrue();
        assertThat(response.getNotificarMenciones()).isFalse();
        assertThat(response.getNotificarInvitaciones()).isTrue();
        assertThat(response.getNotificarAnuncios()).isTrue();
        assertThat(response.getNotificarSolicitudAcceso()).isFalse();
        assertThat(response.getNotificarCambiosDeEventos()).isTrue();
    }

    // ================================================================
    // actualizarPreferencias
    // ================================================================

    @Test
    void actualizarPreferenciasShouldUpdateOnlyProvidedFields() {
        PreferenciasNotificacion existing = new PreferenciasNotificacion();
        existing.setId(1L);
        existing.setEmailsActivados(true);
        existing.setRecordatorio24h(true);
        existing.setRecordatorio1h(true);
        existing.setRecordatorio30min(false);
        existing.setCanalAlarmasPorDefecto(TipoCanal.AMBOS);
        existing.setNotificarMensajeComunidad(false);
        existing.setNotificarMenciones(false);
        existing.setNotificarInvitaciones(false);
        existing.setNotificarAnuncios(false);
        existing.setNotificarSolicitudAcceso(false);
        existing.setNotificarCambiosDeEventos(false);

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.of(existing));
        when(preferenciasRepository.save(any(PreferenciasNotificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferenciasRequest request = new UpdatePreferenciasRequest();
        request.setEmailsActivados(false);
        request.setRecordatorio30min(true);
        request.setCanalAlarmasPorDefecto(TipoCanal.EMAIL);

        PreferenciasNotificacionResponse response =
                preferenciasService.actualizarPreferencias(10L, request);

        assertThat(response.getEmailsActivados()).isFalse();
        assertThat(response.getRecordatorio24h()).isTrue();
        assertThat(response.getRecordatorio30min()).isTrue();
        assertThat(response.getCanalAlarmasPorDefecto()).isEqualTo(TipoCanal.EMAIL);
    }

    @Test
    void actualizarPreferenciasShouldUpdateAllFieldsWhenAllProvided() {
        PreferenciasNotificacion existing = new PreferenciasNotificacion();
        existing.setId(1L);
        existing.setEmailsActivados(false);
        existing.setRecordatorio24h(false);
        existing.setRecordatorio1h(false);
        existing.setRecordatorio30min(false);
        existing.setNotificarMensajeComunidad(false);
        existing.setNotificarMenciones(false);
        existing.setNotificarInvitaciones(false);
        existing.setNotificarAnuncios(false);
        existing.setNotificarSolicitudAcceso(false);
        existing.setNotificarCambiosDeEventos(false);

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.of(existing));
        when(preferenciasRepository.save(any(PreferenciasNotificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferenciasRequest request = new UpdatePreferenciasRequest();
        request.setEmailsActivados(true);
        request.setRecordatorio24h(true);
        request.setRecordatorio1h(true);
        request.setRecordatorio30min(true);
        request.setCanalAlarmasPorDefecto(TipoCanal.PLATAFORMA);
        request.setNotificarMensajeComunidad(true);
        request.setNotificarMenciones(true);
        request.setNotificarInvitaciones(true);
        request.setNotificarAnuncios(true);
        request.setNotificarSolicitudAcceso(true);
        request.setNotificarCambiosDeEventos(true);

        PreferenciasNotificacionResponse response =
                preferenciasService.actualizarPreferencias(10L, request);

        assertThat(response.getEmailsActivados()).isTrue();
        assertThat(response.getRecordatorio24h()).isTrue();
        assertThat(response.getRecordatorio1h()).isTrue();
        assertThat(response.getRecordatorio30min()).isTrue();
        assertThat(response.getCanalAlarmasPorDefecto()).isEqualTo(TipoCanal.PLATAFORMA);
        assertThat(response.getNotificarMensajeComunidad()).isTrue();
        assertThat(response.getNotificarMenciones()).isTrue();
        assertThat(response.getNotificarInvitaciones()).isTrue();
        assertThat(response.getNotificarAnuncios()).isTrue();
        assertThat(response.getNotificarSolicitudAcceso()).isTrue();
        assertThat(response.getNotificarCambiosDeEventos()).isTrue();
    }

    @Test
    void actualizarPreferenciasShouldNotChangeAnythingWhenAllFieldsNull() {
        PreferenciasNotificacion existing = new PreferenciasNotificacion();
        existing.setId(1L);
        existing.setEmailsActivados(true);
        existing.setRecordatorio24h(false);
        existing.setRecordatorio1h(false);
        existing.setRecordatorio30min(false);
        existing.setNotificarMensajeComunidad(false);
        existing.setNotificarMenciones(false);
        existing.setNotificarInvitaciones(false);
        existing.setNotificarAnuncios(false);
        existing.setNotificarSolicitudAcceso(false);
        existing.setNotificarCambiosDeEventos(false);

        when(preferenciasRepository.findByUsuarioId(10L)).thenReturn(Optional.of(existing));
        when(preferenciasRepository.save(any(PreferenciasNotificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferenciasRequest request = new UpdatePreferenciasRequest();

        PreferenciasNotificacionResponse response =
                preferenciasService.actualizarPreferencias(10L, request);

        assertThat(response.getEmailsActivados()).isTrue();
        assertThat(response.getRecordatorio24h()).isFalse();
    }
}
