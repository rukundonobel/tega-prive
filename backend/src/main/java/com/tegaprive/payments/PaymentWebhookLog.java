package com.tegaprive.payments;

import java.time.Instant;

import com.tegaprive.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_webhooks")
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookLog extends BaseEntity {

  @Column(nullable = false)
  private String provider;

  @Column(name = "provider_reference")
  private String providerReference;

  @Column(name = "internal_reference")
  private String internalReference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
  private String rawPayload;

  @Column(name = "signature_valid", nullable = false)
  private boolean signatureValid = false;

  @Column(nullable = false)
  private boolean processed = false;

  @Column(name = "processing_error")
  private String processingError;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;
}
