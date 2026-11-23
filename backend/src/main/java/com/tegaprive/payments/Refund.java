package com.tegaprive.payments;

import java.math.BigDecimal;
import java.util.UUID;

import com.tegaprive.bookings.Booking;
import com.tegaprive.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
public class Refund extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column
  private String reason;

  @Column(nullable = false)
  private String status = "PENDING";

  @Column(name = "initiated_by")
  private UUID initiatedBy;

  @Column(name = "provider_reference")
  private String providerReference;
}
