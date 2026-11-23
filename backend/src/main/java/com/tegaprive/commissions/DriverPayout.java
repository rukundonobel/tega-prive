package com.tegaprive.commissions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tegaprive.common.BaseEntity;
import com.tegaprive.drivers.Driver;
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
@Table(name = "driver_payouts")
@Getter
@Setter
@NoArgsConstructor
public class DriverPayout extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column
  private String provider;

  @Column(name = "provider_reference")
  private String providerReference;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(nullable = false)
  private String status = "PENDING";

  @Column(name = "initiated_by")
  private UUID initiatedBy;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "paid_at")
  private Instant paidAt;
}
