package com.tegaprive.vehicles;

import java.time.Instant;
import java.util.UUID;

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

import com.tegaprive.common.BaseEntity;
import com.tegaprive.drivers.Driver;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @Column(nullable = false)
  private String make;

  @Column(nullable = false)
  private String model;

  @Column(name = "manufacturing_year")
  private Integer manufacturingYear;

  @Column(nullable = false)
  private String colour;

  @Column(name = "registration_number", nullable = false, unique = true)
  private String registrationNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VehicleCategory category;

  @Column(name = "seat_capacity", nullable = false)
  private int seatCapacity;

  @Column(name = "luggage_capacity", nullable = false)
  private int luggageCapacity;

  @Column(name = "has_ac", nullable = false)
  private boolean hasAc;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", nullable = false)
  private VehicleApprovalStatus approvalStatus = VehicleApprovalStatus.DRAFT;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_by")
  private UUID approvedBy;
}
