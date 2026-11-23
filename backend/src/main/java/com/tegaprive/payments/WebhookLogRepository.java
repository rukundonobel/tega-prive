package com.tegaprive.payments;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookLogRepository extends JpaRepository<PaymentWebhookLog, UUID> {

  boolean existsByInternalReferenceAndProcessedTrue(String internalReference);
}
