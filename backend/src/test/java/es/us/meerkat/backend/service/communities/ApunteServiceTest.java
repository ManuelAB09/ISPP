package es.us.meerkat.backend.service.communities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.communities.ApunteResponse;
import es.us.meerkat.backend.entity.communities.Apunte;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ApunteRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ApunteServiceTest {

    @Mock private ApunteRepository apunteRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private MultipartFile file;

    @InjectMocks private ApunteService apunteService;

    private Comunidad comunidad;
    private Usuario usuario;
    private Apunte apunte;

    @BeforeEach
    void setUp() {
        comunidad = Comunidad.builder().id(10L).nombre("Comunidad 10").build();

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario 1");
        usuario.setFoto("foto.jpg");

        apunte =
                Apunte.builder()
                        .id(100L)
                        .titulo("Tema 1")
                        .descripcion("Resumen")
                        .contenido(new byte[] {1, 2, 3})
                        .nombreArchivo("tema1.pdf")
                        .tipoMime("application/pdf")
                        .tamanioArchivo(2048L)
                        .comunidad(comunidad)
                        .usuario(usuario)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .descargas(2)
                        .valoracionMedia(4.5)
                        .build();
    }

    @Test
    void subirApunteShouldCreateSuccessfullyWhenDataIsValid() throws Exception {
        byte[] contenido = "contenido".getBytes();

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getBytes()).thenReturn(contenido);
        when(file.getOriginalFilename()).thenReturn("tema1.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(authorizationService.canParticipate(1L, 10L)).thenReturn(true);
        when(apunteRepository.save(any(Apunte.class)))
                .thenAnswer(
                        invocation -> {
                            Apunte apunte = invocation.getArgument(0);
                            apunte.setId(100L);
                            return apunte;
                        });

        ApunteResponse result = apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitulo()).isEqualTo("Tema 1");
        assertThat(result.getDescripcion()).isEqualTo("Resumen");
        assertThat(result.getNombreArchivo()).isEqualTo("tema1.pdf");
        assertThat(result.getTipoMime()).isEqualTo("application/pdf");
        assertThat(result.getTamanioArchivo()).isEqualTo(2048L);
        assertThat(result.getUsuarioId()).isEqualTo(1L);
        assertThat(result.getUsuarioNombre()).isEqualTo("Usuario 1");
        assertThat(result.getComunidadId()).isEqualTo(10L);
        verify(apunteRepository).save(any(Apunte.class));
    }

    @Test
    void subirApunteShouldUseOriginalFilenameAsTitleWhenTitleIsBlank() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(512L);
        when(file.getBytes()).thenReturn(new byte[] {1, 2, 3});
        when(file.getOriginalFilename()).thenReturn("tema2.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(authorizationService.canParticipate(1L, 10L)).thenReturn(true);
        when(apunteRepository.save(any(Apunte.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApunteResponse result = apunteService.subirApunte(10L, 1L, "   ", null, file);

        assertThat(result.getTitulo()).isEqualTo("tema2.pdf");
    }

    @Test
    void subirApunteShouldFailWhenFileIsEmpty() {
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("archivo no puede estar");

        verify(apunteRepository, never()).save(any(Apunte.class));
    }

    @Test
    void subirApunteShouldFailWhenFileExceedsLimit() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(101L * 1024L * 1024L);

        assertThatThrownBy(() -> apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no puede superar 100 MB");

        verify(apunteRepository, never()).save(any(Apunte.class));
    }

    @Test
    void subirApunteShouldFailWhenCommunityDoesNotExist() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comunidad no encontrada");
    }

    @Test
    void subirApunteShouldFailWhenUserDoesNotExist() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void subirApunteShouldFailWhenReadingFileThrowsException() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getBytes()).thenThrow(new RuntimeException("boom"));
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(authorizationService.canParticipate(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> apunteService.subirApunte(10L, 1L, "Tema 1", "Resumen", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error al procesar el archivo");
    }

    @Test
    void obtenerApuntesComunidadShouldReturnPagedResponses() {
        Page<Apunte> page = new PageImpl<>(List.of(apunte), PageRequest.of(0, 10), 1);
        when(comunidadRepository.existsById(10L)).thenReturn(true);
        when(apunteRepository.findByComunidadIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<ApunteResponse> result =
                apunteService.obtenerApuntesComunidad(10L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getUsuarioNombre()).isEqualTo("Usuario 1");
    }

    @Test
    void obtenerApuntesComunidadShouldFailWhenCommunityDoesNotExist() {
        when(comunidadRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> apunteService.obtenerApuntesComunidad(10L, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comunidad no encontrada");
    }

    @Test
    void obtenerTodosApuntesComunidadShouldReturnMappedList() {
        when(comunidadRepository.existsById(10L)).thenReturn(true);
        when(apunteRepository.findByComunidadIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(apunte));

        List<ApunteResponse> result = apunteService.obtenerTodosApuntesComunidad(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombreArchivo()).isEqualTo("tema1.pdf");
    }

    @Test
    void obtenerApunteShouldReturnMappedResponseWhenExists() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));

        ApunteResponse result = apunteService.obtenerApunte(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getComunidadId()).isEqualTo(10L);
    }

    @Test
    void obtenerApunteShouldFailWhenNotFound() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apunteService.obtenerApunte(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apunte no encontrado");
    }

    @Test
    void descargarApunteShouldReturnContentAndIncrementDownloads() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));
        when(apunteRepository.save(any(Apunte.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        byte[] result = apunteService.descargarApunte(100L);

        assertThat(result).isEqualTo(apunte.getContenido());
        assertThat(apunte.getDescargas()).isEqualTo(3);
        verify(apunteRepository).save(apunte);
    }

    @Test
    void descargarApunteShouldInitializeDownloadsWhenNull() {
        apunte.setDescargas(null);
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));
        when(apunteRepository.save(any(Apunte.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        apunteService.descargarApunte(100L);

        assertThat(apunte.getDescargas()).isEqualTo(1);
    }

    @Test
    void eliminarApunteShouldDeleteWhenOwnerMatches() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));

        apunteService.eliminarApunte(100L, 1L);

        verify(apunteRepository).delete(apunte);
    }

    @Test
    void eliminarApunteShouldDeleteWhenUserIsAdmin() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));
        when(authorizationService.isAdminOf(99L, 10L)).thenReturn(true);

        apunteService.eliminarApunte(100L, 99L);

        verify(apunteRepository).delete(apunte);
    }

    @Test
    void eliminarApunteShouldFailWhenUserHasNoPermissions() {
        when(apunteRepository.findById(100L)).thenReturn(Optional.of(apunte));
        when(authorizationService.isAdminOf(99L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> apunteService.eliminarApunte(100L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    void buscarApuntesShouldReturnMappedPage() {
        Page<Apunte> page = new PageImpl<>(List.of(apunte), PageRequest.of(0, 10), 1);
        when(comunidadRepository.existsById(10L)).thenReturn(true);
        when(apunteRepository.searchByTituloInComunidad(10L, "Tema", PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<ApunteResponse> result =
                apunteService.buscarApuntes(10L, "Tema", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitulo()).isEqualTo("Tema 1");
    }

    @Test
    void obtenerApuntesUsuarioShouldReturnMappedList() {
        when(comunidadRepository.existsById(10L)).thenReturn(true);
        when(apunteRepository.findByComunidadIdAndUsuarioIdOrderByCreatedAtDesc(10L, 1L))
                .thenReturn(List.of(apunte));

        List<ApunteResponse> result = apunteService.obtenerApuntesUsuario(10L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsuarioFoto()).isEqualTo("foto.jpg");
    }
}
