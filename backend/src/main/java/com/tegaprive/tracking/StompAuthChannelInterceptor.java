package com.tegaprive.tracking;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.security.JwtTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * Authenticates every inbound STOMP frame using the JWT in the 'Authorization' header.
 *
 * CONNECT: extracts JWT and sets the STOMP principal. Rejects if invalid/missing.
 *
 * SUBSCRIBE to /topic/journey/{id}/location:
 *   - Rejects if the subscribing user does not have an active CONFIRMED booking
 *     for that journey. Drivers are permitted to subscribe to their own journey.
 *   - This is the authorisation gate that prevents driver locations from being
 *     exposed publicly.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
  private static final Pattern JOURNEY_TOPIC =
      Pattern.compile("^/topic/journey/([0-9a-fA-F-]{36})/location$");

  private final JwtTokenProvider tokenProvider;
  private final BookingRepository bookingRepository;

  public StompAuthChannelInterceptor(JwtTokenProvider tokenProvider,
      BookingRepository bookingRepository) {
    this.tokenProvider = tokenProvider;
    this.bookingRepository = bookingRepository;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String token = extractToken(accessor);
      if (token == null) {
        log.warn("STOMP CONNECT rejected: no token");
        throw new org.springframework.security.access.AccessDeniedException("No JWT provided");
      }
      try {
        Jws<Claims> jws = tokenProvider.parse(token);
        UUID userId = tokenProvider.getUserId(jws);
        List<String> roles = tokenProvider.getRoles(jws);
        var authorities = roles.stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, authorities));
      } catch (Exception e) {
        log.warn("STOMP CONNECT rejected: invalid token: {}", e.getMessage());
        throw new org.springframework.security.access.AccessDeniedException("Invalid JWT");
      }
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      String destination = accessor.getDestination();
      if (destination == null) {
        return message;
      }
      Matcher m = JOURNEY_TOPIC.matcher(destination);
      if (m.matches()) {
        UUID journeyId = UUID.fromString(m.group(1));
        UUID userId = userId(accessor);
        if (userId == null) {
          throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        // Allow if the user has an active confirmed booking OR is the driver of this journey
        boolean allowed = bookingRepository.findAll().stream().anyMatch(b ->
            b.getJourney().getId().equals(journeyId)
            && b.getPassenger().getId().equals(userId)
            && b.getStatus() == BookingStatus.CONFIRMED);

        if (!allowed) {
          // Check driver ownership (driver may subscribe to their own journey)
          boolean isDriver = bookingRepository.findAll().stream().anyMatch(b ->
              b.getJourney().getId().equals(journeyId)
              && b.getJourney().getDriver().getUser().getId().equals(userId));
          if (!isDriver) {
            log.warn("STOMP SUBSCRIBE rejected for user={} journey={}", userId, journeyId);
            throw new org.springframework.security.access.AccessDeniedException(
                "No confirmed booking for this journey");
          }
        }
      }
    }

    return message;
  }

  private String extractToken(StompHeaderAccessor accessor) {
    String auth = accessor.getFirstNativeHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      return auth.substring(7).trim();
    }
    // Fallback: token header (used by some STOMP clients)
    return accessor.getFirstNativeHeader("token");
  }

  private UUID userId(StompHeaderAccessor accessor) {
    if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken token
        && token.getPrincipal() instanceof UUID id) {
      return id;
    }
    return null;
  }
}
