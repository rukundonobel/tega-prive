package com.tegaprive.drivers.dto;

import java.time.Instant;

/** Compact driver view for admin lists. */
public record DriverSummaryDto(
    String id,
    String userId,
    String name,
    String email,
    String licenceNumber,
    String verificationStatus,
    boolean verified,
    Instant createdAt) {
}
