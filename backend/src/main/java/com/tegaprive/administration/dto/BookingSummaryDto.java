package com.tegaprive.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingSummaryDto(
    String id,
    String origin,
    String destination,
    String passengerName,
    String driverName,
    int seatsBooked,
    BigDecimal totalAmount,
    String status,
    Instant createdAt) {
}
