package com.tegaprive.reviews.dto;

import java.time.Instant;

public record ReviewDto(
    String id,
    String bookingId,
    String reviewerName,
    String driverId,
    String driverName,
    int rating,
    String comment,
    boolean hidden,
    Instant createdAt) {
}
