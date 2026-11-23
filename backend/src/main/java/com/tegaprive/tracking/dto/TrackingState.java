package com.tegaprive.tracking.dto;

import java.math.BigDecimal;

/**
 * Snapshot of the journey's current tracked state — returned by REST GET
 * on page load so the passenger doesn't wait for the first WebSocket message.
 */
public record TrackingState(
    String journeyId,
    String journeyStatus,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal heading,
    BigDecimal speed,
    long lastUpdatedAt,     // epoch-milliseconds; 0 means no fix yet
    boolean fresh) {        // true if lastUpdatedAt is within 60 seconds
}
