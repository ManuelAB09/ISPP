package es.us.meerkat.backend.service.forms;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.communities.ComentarioAnuncioResponse;
import es.us.meerkat.backend.dto.communities.CreateComentarioAnuncioRequest;
import es.us.meerkat.backend.dto.users.UserSimpleResponse;
import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.communities.ComentarioAnuncio;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.AnuncioRepository;
import es.us.meerkat.backend.repository.communities.ComentarioAnuncioRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ComentarioAnuncioService {
    private final ComentarioAnuncioRepository comentarioRepo;
    private final AnuncioRepository anuncioRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuthorizationService authorizationService;

    public ComentarioAnuncioResponse crearComentario(
            Long anuncioId, Long userId, CreateComentarioAnuncioRequest req) {
        Anuncio anuncio =
                anuncioRepo
                        .findById(anuncioId)
                        .orElseThrow(() -> new IllegalArgumentException("Anuncio no encontrado"));
        if (!Boolean.TRUE.equals(anuncio.getPermitirComentarios())) {
            throw new IllegalArgumentException(
                    "Los comentarios estan desactivados en este anuncio");
        }
        if (authorizationService != null
                && anuncio.getComunidad() != null
                && !authorizationService.canParticipate(userId, anuncio.getComunidad().getId())) {
            throw new IllegalArgumentException(
                    "Debes ser miembro para comentar en una comunidad privada");
        }
        Usuario usuario =
                usuarioRepo
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        ComentarioAnuncio comentario =
                ComentarioAnuncio.builder()
                        .anuncio(anuncio)
                        .usuario(usuario)
                        .texto(req.texto())
                        .build();
        ComentarioAnuncio saved = comentarioRepo.save(comentario);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ComentarioAnuncioResponse> listarComentarios(Long anuncioId) {
        Anuncio anuncio =
                anuncioRepo
                        .findById(anuncioId)
                        .orElseThrow(() -> new IllegalArgumentException("Anuncio no encontrado"));
        return comentarioRepo.findByAnuncioOrderByCreatedAtDesc(anuncio).stream()
                .map(this::toResponse)
                .toList();
    }

    public ComentarioAnuncioResponse toResponse(ComentarioAnuncio c) {
        return new ComentarioAnuncioResponse(
                c.getId(),
                c.getTexto(),
                new UserSimpleResponse(
                        c.getUsuario().getId(),
                        c.getUsuario().getNombre(),
                        c.getUsuario().getEmail(), // Ajusta si el campo es diferente
                        c.getUsuario().getFoto() // Ajusta si el campo es diferente
                        ),
                c.getCreatedAt());
    }
}
