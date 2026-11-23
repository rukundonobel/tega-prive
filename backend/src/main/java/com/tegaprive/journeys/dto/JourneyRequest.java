package com.tegaprive.journeys.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record JourneyRequest(
    @NotNull UUID vehicleId,
    @NotNull UUID originId,
    @NotNull UUID destinationId,
    @NotNull @Future Instant departureTime,
    Instant estimatedArrival,
    @NotNull @Positive BigDecimal pricePerSeat,
    @Min(1) int totalSeats,
    String pickupInstructions,
    String dropoffInstructions,
    String notes) {
}
