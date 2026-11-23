package com.tegaprive.payments.dto;

/** Generic webhook payload — both MTN and Airtel callbacks are mapped to this shape. */
public record WebhookPayload(
    String internalReference,    // our reference id
    String providerReference,    // provider's transaction id
    String status,               // SUCCESSFUL | FAILED | PENDING
    String failureReason) {
}
