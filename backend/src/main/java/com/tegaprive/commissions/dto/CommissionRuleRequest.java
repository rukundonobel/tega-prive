package com.tegaprive.commissions.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CommissionRuleRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("0.99") BigDecimal driverCommissionPct,
    @NotNull @DecimalMin("0.00") @DecimalMax("0.99") BigDecimal passengerServiceFeePct,
    BigDecimal minServiceFee,
    BigDecimal maxServiceFee,
    BigDecimal fixedBookingFee,
    BigDecimal cancellationFeePct,
    String note) {
}
