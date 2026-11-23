package com.tegaprive.bookings.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingDto(
    String id,
    String journeyId,
    String passengerId,
    String passengerName,
    String passengerPhone,
    // Journey summary
    String origin,
    String destination,
    Instant departureTime,
    // Booking detail
    int seatsBooked,
    String status,
    BigDecimal grossFare,
    BigDecimal serviceFee,
    BigDecimal totalAmount,
    String paymentMethod,
    Instant expiresAt,
    Instant confirmedAt,
    Instant cancelledAt,
    String cancelReason,
    Instant createdAt) {
}
