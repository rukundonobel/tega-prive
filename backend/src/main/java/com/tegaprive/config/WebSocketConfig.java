package com.tegaprive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.tegaprive.config.CorsProperties;
import com.tegaprive.tracking.StompAuthChannelInterceptor;

/**
 * WebSocket / STOMP configuration.
 *
 * Clients connect to ws://localhost:8080/ws (SockJS fallback available).
 *
 * Message flow:
 *   DRIVER sends   → /app/journey/{id}/location  (handled by @MessageMapping)
 *   PASSENGER subs → /topic/journey/{id}/location (broadcast by SimpMessagingTemplate)
 *
 * Authorization is enforced by StompAuthChannelInterceptor:
 *   - Every CONNECT frame must carry a valid JWT (passed as STOMP header 'Authorization').
 *   - Every SUBSCRIBE to /topic/journey/{id}/location is checked against the booking DB.
 *   - Driver send authorisation is checked in the @MessageMapping handler.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final CorsProperties corsProperties;
  private final StompAuthChannelInterceptor authInterceptor;

  public WebSocketConfig(CorsProperties corsProperties, StompAuthChannelInterceptor authInterceptor) {
    this.corsProperties = corsProperties;
    this.authInterceptor = authInterceptor;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(new String[0]))
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(authInterceptor);
  }
}
