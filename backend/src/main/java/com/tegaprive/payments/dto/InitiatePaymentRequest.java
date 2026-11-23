package com.tegaprive.payments.dto;

import java.util.UUID;

import com.tegaprive.payments.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(
    @NotNull UUID bookingId,
    @NotNull PaymentProvider provider,
    @NotBlank String phoneNumber) {
}
