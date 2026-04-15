package es.us.meerkat.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.Institution;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.Apunte;
import es.us.meerkat.backend.entity.communities.Categoria;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ApunteRepository;
import es.us.meerkat.backend.repository.communities.CategoriaRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.maps.UbicacionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private TutorRepository tutorRepo;
    @Mock private UbicacionRepository ubicacionRepo;
    @Mock private ComunidadRepository comunidadRepo;
    @Mock private MiembroComunidadRepository miembroRepo;
    @Mock private CategoriaRepository categoriaRepo;
    @Mock private EventoRepository eventoRepo;
    @Mock private AsistenciaEventoRepository asistenciaRepo;
    @Mock private InstitutionRepository institutionRepo;
    @Mock private ApunteRepository apunteRepo;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @Test
    void seedDatabaseShouldSkipWhenUsersAlreadyExist() throws Exception {
        DataSeeder seeder = new DataSeeder();
        when(usuarioRepo.count()).thenReturn(3L);

        CommandLineRunner runner =
                seeder.seedDatabase(
                        usuarioRepo,
                        tutorRepo,
                        ubicacionRepo,
                        comunidadRepo,
                        miembroRepo,
                        categoriaRepo,
                        eventoRepo,
                        asistenciaRepo,
                        institutionRepo,
                        apunteRepo,
                        passwordEncoder);

        runner.run();

        verify(usuarioRepo).count();
        verify(usuarioRepo, never()).saveAll(anyList());
        verifyNoInteractions(
                tutorRepo,
                ubicacionRepo,
                comunidadRepo,
                miembroRepo,
                categoriaRepo,
                eventoRepo,
                asistenciaRepo,
                institutionRepo,
                apunteRepo,
                passwordEncoder);
    }

    @Test
    void seedDatabaseShouldInsertAllSeedDataWhenDatabaseIsEmpty() throws Exception {
        DataSeeder seeder = new DataSeeder();
        when(usuarioRepo.count()).thenReturn(0L);

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "enc-" + invocation.getArgument(0, String.class));

        when(usuarioRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(ubicacionRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(comunidadRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(categoriaRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(miembroRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(eventoRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(asistenciaRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(tutorRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(apunteRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(institutionRepo.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Institution.class));

        CommandLineRunner runner =
                seeder.seedDatabase(
                        usuarioRepo,
                        tutorRepo,
                        ubicacionRepo,
                        comunidadRepo,
                        miembroRepo,
                        categoriaRepo,
                        eventoRepo,
                        asistenciaRepo,
                        institutionRepo,
                        apunteRepo,
                        passwordEncoder);

        runner.run();

        verify(usuarioRepo).count();
        verify(passwordEncoder, atLeast(8)).encode(anyString());
        verify(usuarioRepo, times(2)).saveAll(anyList());
        verify(usuarioRepo).flush();

        verify(usuarioRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Usuario> list) ->
                                        list.size() == 8
                                                && list.stream()
                                                        .anyMatch(
                                                                u ->
                                                                        "admin@meerkat.es"
                                                                                .equals(
                                                                                        u
                                                                                                .getEmail()))));
        verify(usuarioRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Usuario> list) ->
                                        list.size() == 4
                                                && list.stream()
                                                        .allMatch(u -> u.getUbicacion() != null)));

        verify(ubicacionRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Ubicacion> list) -> list.size() == 5));
        verify(comunidadRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Comunidad> list) -> list.size() == 5));
        verify(categoriaRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Categoria> list) -> list.size() == 8));
        verify(miembroRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<MiembroComunidad> list) -> list.size() == 15));
        verify(eventoRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Evento> list) -> list.size() == 8));
        verify(asistenciaRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<AsistenciaEvento> list) -> list.size() == 14));
        verify(tutorRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Tutor> list) -> list.size() == 4));
        verify(apunteRepo)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                (List<Apunte> list) -> list.size() == 5));
        verify(institutionRepo).save(any(Institution.class));

        assertThat(true).isTrue();
    }
}
