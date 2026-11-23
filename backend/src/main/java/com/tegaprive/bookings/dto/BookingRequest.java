package com.tegaprive.bookings.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull UUID journeyId,
    @Min(1) int seatsRequested) {
}
