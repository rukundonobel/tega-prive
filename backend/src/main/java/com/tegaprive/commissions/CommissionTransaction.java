package com.tegaprive.commissions;

import java.math.BigDecimal;

import com.tegaprive.bookings.Booking;
import com.tegaprive.common.BaseEntity;
import com.tegaprive.payments.Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable record of how money was split for a paid booking. Never updated. */
@Entity
@Table(name = "commission_transactions")
@Getter
@Setter
@NoArgsConstructor
public class CommissionTransaction extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commission_rule_id", nullable = false)
  private CommissionRule rule;

  @Column(name = "gross_fare", nullable = false)
  private BigDecimal grossFare;

  /** Amount deducted from driver (goes to platform). */
  @Column(name = "driver_commission", nullable = false)
  private BigDecimal driverCommission;

  /** Amount charged on top of passenger fare (goes to platform). */
  @Column(name = "passenger_service_fee", nullable = false)
  private BigDecimal passengerServiceFee;

  /** What the driver actually receives: grossFare - driverCommission. */
  @Column(name = "driver_net", nullable = false)
  private BigDecimal driverNet;

  /** Total platform revenue: driverCommission + passengerServiceFee. */
  @Column(name = "platform_revenue", nullable = false)
  private BigDecimal platformRevenue;
}
