package com.redditclone.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers STOMP endpoints for the WebSocket server.
     * @param registry The registry to register endpoints.
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // The endpoint "/ws" will be used by the frontend to connect to the WebSocket server.
        // `withSockJS()` enables SockJS fallback options for browsers that don't support WebSockets.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    /**
     * Configures the message broker.
     * @param config The registry to configure the message broker.
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // "/app" is the prefix for messages coming from clients to the server.
        // e.g., messages to @MessageMapping("/chat") will go to /app/chat
        config.setApplicationDestinationPrefixes("/app");

        // "/topic" and "/queue" are prefixes for messages routed back to the client.
        // "/topic" for public broadcasts (e.g., /topic/public-chat)
        // "/user" for private messages (e.g., /user/{userId}/queue/messages)
        config.enableSimpleBroker("/topic", "/user");

        // This ensures messages to /user/<userId>/queue/messages are routed to the specific user.
        config.setUserDestinationPrefix("/user");
    }
}