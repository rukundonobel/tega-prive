package com.tegaprive.commissions.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverEarningDto(
    String id,
    String driverId,
    String driverName,
    String bookingId,
    BigDecimal grossFare,
    BigDecimal commission,
    BigDecimal netEarnings,
    String settlementStatus,
    Instant createdAt) {
}
