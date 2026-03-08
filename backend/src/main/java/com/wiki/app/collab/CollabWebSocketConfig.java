package com.wiki.app.collab;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CollabWebSocketConfig implements WebSocketConfigurer {
    private final CollabWebSocketHandler collabWebSocketHandler;

    public CollabWebSocketConfig(CollabWebSocketHandler collabWebSocketHandler) {
        this.collabWebSocketHandler = collabWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(collabWebSocketHandler, "/ws/collab")
                .setAllowedOriginPatterns("*");
    }
}
