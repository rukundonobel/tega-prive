package com.tegaprive.commissions;

import java.math.BigDecimal;
import java.util.UUID;

import com.tegaprive.bookings.Booking;
import com.tegaprive.common.BaseEntity;
import com.tegaprive.drivers.Driver;
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

@Entity
@Table(name = "driver_earnings")
@Getter
@Setter
@NoArgsConstructor
public class DriverEarning extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @Column(name = "gross_fare", nullable = false)
  private BigDecimal grossFare;

  @Column(nullable = false)
  private BigDecimal commission;

  @Column(name = "net_earnings", nullable = false)
  private BigDecimal netEarnings;

  @Column(name = "settlement_status", nullable = false)
  private String settlementStatus = "PENDING";

  @Column(name = "payout_id")
  private UUID payoutId;
}
