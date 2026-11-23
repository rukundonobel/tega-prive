package com.tegaprive.commissions.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRuleDto(
    String id,
    BigDecimal driverCommissionPct,
    BigDecimal passengerServiceFeePct,
    BigDecimal minServiceFee,
    BigDecimal maxServiceFee,
    BigDecimal fixedBookingFee,
    BigDecimal cancellationFeePct,
    Instant effectiveFrom,
    String note) {
}
