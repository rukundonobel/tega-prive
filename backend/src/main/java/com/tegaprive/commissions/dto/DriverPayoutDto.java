package com.tegaprive.commissions.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverPayoutDto(
    String id,
    String driverId,
    String driverName,
    BigDecimal amount,
    String provider,
    String phoneNumber,
    String status,
    Instant paidAt,
    Instant createdAt) {
}
