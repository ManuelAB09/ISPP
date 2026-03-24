package es.us.meerkat.backend.security;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Custom handshake handler that extracts the JWT from a query parameter and sets the user Principal
 * on the WebSocket session. This is required for SimpUserRegistry to track connected users, which
 * in turn enables convertAndSendToUser() message delivery.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected Principal determineUser(
            ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        URI uri = request.getURI();
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");

        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String email = jwtService.extractEmail(token);
            if (email != null && jwtService.isTokenValid(token, email)) {
                Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
                if (usuario != null) {
                    final String userId = usuario.getId().toString();
                    return new UsernamePasswordAuthenticationToken(
                            usuario, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))) {
                        @Override
                        public String getName() {
                            return userId;
                        }
                    };
                }
            }
        } catch (Exception ignored) {
            // Invalid token — fall through to null
        }

        return null;
    }
}
