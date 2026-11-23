package com.tegaprive.journeys;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

  List<Journey> findByDriverId(UUID driverId);

  /**
   * Public journey search: departure after the given time, available seats >= requested,
   * matching origin and destination. Only SCHEDULED journeys appear.
   */
  @Query("""
      SELECT j FROM Journey j
      WHERE j.origin.id = :originId
        AND j.destination.id = :destinationId
        AND j.status = 'SCHEDULED'
        AND j.departureTime >= :from
        AND j.departureTime < :to
        AND j.availableSeats >= :seats
        AND j.deletedAt IS NULL
      ORDER BY j.departureTime ASC
      """)
  List<Journey> search(
      @Param("originId") UUID originId,
      @Param("destinationId") UUID destinationId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("seats") int seats);
}
