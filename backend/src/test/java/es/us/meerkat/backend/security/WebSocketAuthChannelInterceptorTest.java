package es.us.meerkat.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preSendShouldReturnOriginalMessageWhenCommandIsNotConnect() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void preSendShouldThrowWhenAuthorizationHeaderIsMissing() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    void preSendShouldThrowWhenTokenCannotBeParsed() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer bad-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("bad-token")).thenThrow(new RuntimeException("invalid"));

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token format");
    }

    @Test
    void preSendShouldThrowWhenTokenIsInvalidOrExpired() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("token-1")).thenReturn("user@meerkat.es");
        when(jwtService.isTokenValid("token-1", "user@meerkat.es")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    @Test
    void preSendShouldThrowWhenUserDoesNotExistInDatabase() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-2");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("token-2")).thenReturn("user@meerkat.es");
        when(jwtService.isTokenValid("token-2", "user@meerkat.es")).thenReturn(true);
        when(usuarioRepository.findByEmail("user@meerkat.es")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found in database");
    }

    @Test
    void preSendShouldAuthenticateAndAttachPrincipalForValidConnectFrame() {
        WebSocketAuthChannelInterceptor interceptor =
                new WebSocketAuthChannelInterceptor(jwtService, usuarioRepository);

        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setEmail("ok@meerkat.es");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-ok");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractEmail("token-ok")).thenReturn("ok@meerkat.es");
        when(jwtService.isTokenValid("token-ok", "ok@meerkat.es")).thenReturn(true);
        when(usuarioRepository.findByEmail("ok@meerkat.es")).thenReturn(Optional.of(usuario));

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isNotNull();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("99");

        StompHeaderAccessor wrapped = StompHeaderAccessor.wrap(result);
        assertThat(wrapped.getUser()).isNotNull();
    }
}
