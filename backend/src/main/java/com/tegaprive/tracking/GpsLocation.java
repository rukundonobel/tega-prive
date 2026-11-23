package com.tegaprive.tracking;

import java.math.BigDecimal;
import java.time.Instant;

import com.tegaprive.drivers.Driver;
import com.tegaprive.journeys.Journey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Individual GPS fix from the driver's device.
 *
 * The PostGIS `geography` column is intentionally stored as a plain String
 * in the entity — Hibernate + H2 cannot handle the PostGIS type at schema
 * creation time. The actual ST_MakePoint insert is done via a @Query / native
 * SQL in the repository so production still benefits from spatial indexing,
 * while tests use the plain lat/lon columns.
 *
 * Future React Native app sends to the same STOMP endpoint; device='MOBILE_APP'.
 */
@Entity
@Table(name = "gps_locations")
@Getter
@Setter
@NoArgsConstructor
public class GpsLocation {

  @Id
  @Column(nullable = false, columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "journey_id", nullable = false)
  private Journey journey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  // Plain lat/lon — always populated; used by tests and simple queries.
  @Column(nullable = false, precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(nullable = false, precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(precision = 5, scale = 2)
  private BigDecimal heading;

  @Column(precision = 6, scale = 2)
  private BigDecimal speed;

  @Column(precision = 8, scale = 2)
  private BigDecimal accuracy;

  @Column(nullable = false)
  private String device = "BROWSER";

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt = Instant.now();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
