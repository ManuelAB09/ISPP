package es.us.meerkat.backend.service;

import es.us.meerkat.backend.dto.ComentarioAnuncioResponse;
import es.us.meerkat.backend.dto.CreateComentarioAnuncioRequest;
import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.ComentarioAnuncio;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AnuncioRepository;
import es.us.meerkat.backend.repository.ComentarioAnuncioRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import es.us.meerkat.backend.dto.UserSimpleResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class ComentarioAnuncioService {
        private final ComentarioAnuncioRepository comentarioRepo;
        private final AnuncioRepository anuncioRepo;
        private final UsuarioRepository usuarioRepo;

        public ComentarioAnuncioResponse crearComentario(Long anuncioId, Long userId,
                        CreateComentarioAnuncioRequest req) {
                Anuncio anuncio = anuncioRepo.findById(anuncioId)
                                .orElseThrow(() -> new IllegalArgumentException("Anuncio no encontrado"));
                Usuario usuario = usuarioRepo.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                ComentarioAnuncio comentario = ComentarioAnuncio.builder()
                                .anuncio(anuncio)
                                .usuario(usuario)
                                .texto(req.texto())
                                .build();
                ComentarioAnuncio saved = comentarioRepo.save(comentario);
                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public List<ComentarioAnuncioResponse> listarComentarios(Long anuncioId) {
                Anuncio anuncio = anuncioRepo.findById(anuncioId)
                                .orElseThrow(() -> new IllegalArgumentException("Anuncio no encontrado"));
                return comentarioRepo.findByAnuncioOrderByCreatedAtDesc(anuncio)
                                .stream().map(this::toResponse).toList();
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
