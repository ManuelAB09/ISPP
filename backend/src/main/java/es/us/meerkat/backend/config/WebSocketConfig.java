package es.us.meerkat.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import es.us.meerkat.backend.security.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;

/**
 * Configuración del servidor WebSocket con STOMP.
 *
 * <p>
 * Expone el endpoint `/ws` para conexiones WebSocket y configura el broker en
 * memoria.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    /**
     * Registra los endpoints HTTP para el handshake WebSocket.
     *
     * @param registry registro de endpoints STOMP.
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:3001", "http://localhost:8080")
                .withSockJS()
                .setInterceptors();
    }

    /**
     * Configura prefijos de envío y suscripción.
     *
     * @param registry registro del broker STOMP.
     */
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configura los transportes WebSocket para mejorar compatibilidad con SockJS.
     *
     * @param registration registro de transporte WebSocket.
     */
    @Override
    public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(128 * 1024)
                .setSendTimeLimit(20 * 1000)
                .setSendBufferSizeLimit(512 * 1024);
    }

    /**
     * Registra el interceptor de autenticación para validar JWT en el handshake
     * STOMP.
     *
     * @param registration registro de canales de entrada.
     */
    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
