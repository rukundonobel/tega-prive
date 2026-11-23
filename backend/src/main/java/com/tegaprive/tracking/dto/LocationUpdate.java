package com.tegaprive.tracking.dto;

import java.math.BigDecimal;

/**
 * GPS update sent by the driver (inbound STOMP message) and
 * also the payload broadcast to passengers (outbound topic message).
 */
public record LocationUpdate(
    String journeyId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal heading,     // 0-360 degrees, null if unavailable
    BigDecimal speed,       // km/h, null if unavailable
    BigDecimal accuracy,    // metres, null if unavailable
    String device,          // BROWSER | MOBILE_APP
    long recordedAt) {      // epoch-milliseconds from the device
}
