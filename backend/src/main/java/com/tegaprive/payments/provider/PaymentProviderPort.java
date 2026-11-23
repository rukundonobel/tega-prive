package com.tegaprive.payments.provider;

import java.math.BigDecimal;

/**
 * Pluggable payment provider interface.
 * Implementations: MtnMomoProviderAdapter, AirtelMoneyProviderAdapter.
 * Adding a new gateway = new adapter + config; booking logic never changes.
 */
public interface PaymentProviderPort {

  /** Human-readable provider name matching the PaymentProvider enum. */
  String providerName();

  /**
   * Initiates a payment request with the provider.
   * @return InitiateResult containing the provider's transaction reference or pending status.
   */
  InitiateResult initiate(InitiateRequest request);

  /**
   * Queries the provider for the current payment status.
   * Always called on the backend; frontend confirmation is never trusted.
   */
  VerifyResult verify(String providerReference);

  /** True when this adapter is correctly configured (env vars present). */
  boolean isConfigured();

  record InitiateRequest(
      String internalReference,
      BigDecimal amount,
      String currency,
      String phoneNumber,
      String description) {
  }

  record InitiateResult(
      String providerReference,
      String status,     // PENDING | SUCCESSFUL | FAILED
      String rawPayload) {
  }

  record VerifyResult(
      String status,     // SUCCESSFUL | PENDING | FAILED
      String providerReference,
      String rawPayload) {
  }
}
