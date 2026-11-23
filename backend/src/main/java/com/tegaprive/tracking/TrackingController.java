package com.tegaprive.tracking;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.tracking.dto.LocationUpdate;
import com.tegaprive.tracking.dto.TrackingState;

/**
 * REST + STOMP endpoints for live GPS tracking.
 *
 * REST (HTTP):
 *   GET /api/tracking/journey/{id}/state
 *     → Returns the latest GPS fix for initial page load.
 *     → Requires CONFIRMED booking or driver ownership.
 *
 * STOMP (WebSocket):
 *   DRIVER sends to /app/journey/{id}/location
 *     → Validated, persisted and broadcast to /topic/journey/{id}/location
 *   PASSENGER subscribes to /topic/journey/{id}/location
 *     → Authorisation enforced by StompAuthChannelInterceptor
 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

  private final TrackingService trackingService;

  public TrackingController(TrackingService trackingService) {
    this.trackingService = trackingService;
  }

  /**
   * REST: snapshot of the latest GPS state.
   * Passengers call this on page load; they then subscribe via STOMP for live updates.
   */
  @GetMapping("/journey/{journeyId}/state")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<TrackingState> getState(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID journeyId) {
    return ApiResponse.ok(trackingService.getState(userId, journeyId));
  }

  /**
   * STOMP: driver sends their GPS coordinates.
   * Client sends to /app/journey/{id}/location
   * The service broadcasts to /topic/journey/{id}/location
   */
  @MessageMapping("/journey/{journeyId}/location")
  public void receiveLocation(
      @DestinationVariable String journeyId,
      @Payload LocationUpdate update,
      Principal principal) {
    if (principal == null) {
      return; // StompAuthChannelInterceptor should have rejected unauthenticated CONNECTs
    }
    UUID userId = (UUID) ((org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken) principal).getPrincipal();
    trackingService.ingestLocation(userId, update);
  }
}
