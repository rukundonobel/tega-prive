package com.tegaprive.commissions.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PayoutRequest(
    @NotNull UUID driverId,
    @NotNull @Positive BigDecimal amount,
    String provider,
    String phoneNumber) {
}
