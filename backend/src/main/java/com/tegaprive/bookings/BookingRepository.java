package com.tegaprive.bookings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

  List<Booking> findByPassengerId(UUID passengerId);

  List<Booking> findByJourneyId(UUID journeyId);

  /** Used by the driver to see bookings on their journeys. */
  @Query("""
      SELECT b FROM Booking b
      WHERE b.journey.id = :journeyId
        AND b.journey.driver.id = :driverId
      """)
  List<Booking> findByJourneyIdAndDriverId(
      @Param("journeyId") UUID journeyId,
      @Param("driverId") UUID driverId);

  /** Active booking check: prevents duplicate active bookings for the same passenger+journey. */
  @Query("""
      SELECT b FROM Booking b
      WHERE b.journey.id = :journeyId
        AND b.passenger.id = :passengerId
        AND b.status NOT IN ('CANCELLED', 'REFUNDED', 'EXPIRED')
      """)
  Optional<Booking> findActiveBooking(
      @Param("journeyId") UUID journeyId,
      @Param("passengerId") UUID passengerId);

  /** Count seats already locked by active bookings (not yet released). */
  @Query("""
      SELECT COALESCE(SUM(b.seatsBooked), 0) FROM Booking b
      WHERE b.journey.id = :journeyId
        AND b.status NOT IN ('CANCELLED', 'REFUNDED', 'EXPIRED')
      """)
  int sumActiveSeats(@Param("journeyId") UUID journeyId);

  List<Booking> findByJourneyIdOrderByCreatedAtDesc(UUID journeyId);

  List<Booking> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);
}
