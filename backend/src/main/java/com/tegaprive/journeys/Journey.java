package com.tegaprive.journeys;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;
import com.tegaprive.drivers.Driver;
import com.tegaprive.vehicles.Vehicle;

/**
 * A published journey: a driver + approved vehicle travelling from origin to destination.
 *
 * Overbooking prevention:
 *   available_seats is the live inventory counter.
 *   The @Version field enables optimistic locking — concurrent booking attempts will
 *   retry on conflict rather than silently over-decrement.
 *   A DB CHECK constraint (available_seats >= 0) is the final safety net.
 */
@Entity
@Table(name = "journeys")
@Getter
@Setter
@NoArgsConstructor
public class Journey extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "origin_id", nullable = false)
  private Location origin;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_id", nullable = false)
  private Location destination;

  @Column(name = "departure_time", nullable = false)
  private Instant departureTime;

  @Column(name = "estimated_arrival")
  private Instant estimatedArrival;

  @Column(name = "price_per_seat", nullable = false)
  private BigDecimal pricePerSeat;

  @Column(name = "total_seats", nullable = false)
  private int totalSeats;

  @Column(name = "available_seats", nullable = false)
  private int availableSeats;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JourneyStatus status = JourneyStatus.SCHEDULED;

  @Column(name = "pickup_instructions", columnDefinition = "text")
  private String pickupInstructions;

  @Column(name = "dropoff_instructions", columnDefinition = "text")
  private String dropoffInstructions;

  @Column(columnDefinition = "text")
  private String notes;

  /**
   * Optimistic lock version. Hibernate increments this on every UPDATE so that two
   * concurrent transactions trying to decrement available_seats will conflict and retry,
   * preventing silent overbooking.
   */
  @Version
  @Column(nullable = false)
  private long version = 0;
}
