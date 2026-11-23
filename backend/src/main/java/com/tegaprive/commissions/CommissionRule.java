package com.tegaprive.commissions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tegaprive.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only versioned commission rules. Never mutated; new rows are inserted when rules change. */
@Entity
@Table(name = "commission_rules")
@Getter
@Setter
@NoArgsConstructor
public class CommissionRule extends BaseEntity {

  /** Fraction deducted from driver gross fare. e.g. 0.1000 = 10%. */
  @Column(name = "driver_commission_pct", nullable = false)
  private BigDecimal driverCommissionPct;

  /** Fraction added on top of passenger fare. e.g. 0.0300 = 3%. */
  @Column(name = "passenger_service_fee_pct", nullable = false)
  private BigDecimal passengerServiceFeePct;

  @Column(name = "min_service_fee", nullable = false)
  private BigDecimal minServiceFee = BigDecimal.ZERO;

  @Column(name = "max_service_fee")
  private BigDecimal maxServiceFee;

  @Column(name = "fixed_booking_fee", nullable = false)
  private BigDecimal fixedBookingFee = BigDecimal.ZERO;

  @Column(name = "cancellation_fee_pct", nullable = false)
  private BigDecimal cancellationFeePct = BigDecimal.ZERO;

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom = Instant.now();

  @Column(name = "created_by")
  private UUID createdBy;

  @Column
  private String note;
}
