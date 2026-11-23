package com.tegaprive.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
    String id,
    String bookingId,
    String provider,
    String internalReference,
    String providerReference,
    BigDecimal amount,
    String currency,
    String phoneNumber,
    String status,
    boolean verified,
    String failureReason,
    Instant initiatedAt,
    Instant verifiedAt) {
}
