package com.tegaprive.journeys.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record JourneyDto(
    String id,
    // Driver info
    String driverId,
    String driverName,
    String driverPhotoUrl,
    boolean driverVerified,
    double driverRating,
    int driverRatingCount,
    // Vehicle info
    String vehicleId,
    String vehicleMake,
    String vehicleModel,
    String vehicleCategory,
    int vehicleSeats,
    boolean vehicleHasAc,
    String vehiclePrimaryPhotoUrl,
    // Route
    String originId,
    String originName,
    String destinationId,
    String destinationName,
    // Timing & pricing
    Instant departureTime,
    Instant estimatedArrival,
    BigDecimal pricePerSeat,
    int totalSeats,
    int availableSeats,
    // Status & extras
    String status,
    String pickupInstructions,
    String dropoffInstructions,
    String notes,
    Instant createdAt) {
}
