package com.tegaprive.tracking;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GpsLocationRepository extends JpaRepository<GpsLocation, UUID> {

  /** Most recent GPS fix for a journey — used for initial state on page load. */
  @Query("SELECT g FROM GpsLocation g WHERE g.journey.id = :journeyId ORDER BY g.recordedAt DESC LIMIT 1")
  Optional<GpsLocation> findLatestByJourneyId(@Param("journeyId") UUID journeyId);
}
