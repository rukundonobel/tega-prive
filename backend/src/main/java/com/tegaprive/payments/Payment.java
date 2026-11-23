package com.tegaprive.payments;

import java.math.BigDecimal;
import java.time.Instant;

import com.tegaprive.bookings.Booking;
import com.tegaprive.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentProvider provider;

  @Column(name = "internal_reference", nullable = false, unique = true)
  private String internalReference;

  @Column(name = "provider_reference")
  private String providerReference;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "RWF";

  @Column(name = "phone_number", nullable = false)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status = PaymentStatus.INITIATED;

  @Column(nullable = false)
  private boolean verified = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "provider_payload", columnDefinition = "jsonb")
  private String providerPayload;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "initiated_at", nullable = false)
  private Instant initiatedAt = Instant.now();

  @Column(name = "verified_at")
  private Instant verifiedAt;
}
