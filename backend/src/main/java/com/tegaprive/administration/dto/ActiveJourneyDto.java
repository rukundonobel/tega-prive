package com.tegaprive.administration.dto;

import java.time.Instant;

public record ActiveJourneyDto(
    String journeyId,
    String driverName,
    String vehicleName,
    String registrationNumber,
    String origin,
    String destination,
    Instant departureTime,
    int bookedSeats,
    int totalSeats,
    Double latitude,
    Double longitude,
    Instant lastGpsUpdate) {
}
