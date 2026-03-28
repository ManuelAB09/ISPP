package es.us.meerkat.backend.security;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Interceptor que valida el token JWT en el handshake WebSocket STOMP. */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    /**
     * Intercepta todos los mensajes STOMP y valida JWT en el frame CONNECT.
     *
     * @param message el mensaje STOMP.
     * @param channel canal de mensajes.
     * @return el mensaje si es válido, lanza excepción si no.
     */
    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            final String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            final String token = authHeader.substring(BEARER_PREFIX.length());
            String email = null;

            try {
                email = jwtService.extractEmail(token);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid token format");
            }

            if (email != null && jwtService.isTokenValid(token, email)) {
                final Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

                if (usuario != null) {
                    final String userId = usuario.getId().toString();
                    final UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    usuario,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))) {
                                @Override
                                public String getName() {
                                    return userId;
                                }
                            };

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    accessor.setUser(authToken);
                    accessor.setLeaveMutable(true);
                    return MessageBuilder.createMessage(
                            message.getPayload(), accessor.getMessageHeaders());
                } else {
                    throw new IllegalArgumentException("User not found in database");
                }
            } else {
                throw new IllegalArgumentException("Invalid or expired token");
            }
        }

        return message;
    }
}
