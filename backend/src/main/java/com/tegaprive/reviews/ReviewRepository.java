package com.tegaprive.reviews;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findByDriverId(UUID driverId);

  Optional<Review> findByBookingId(UUID bookingId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.driver.id = :driverId AND r.hidden = false")
  Double averageRatingByDriver(@Param("driverId") UUID driverId);

  @Query("SELECT COUNT(r) FROM Review r WHERE r.driver.id = :driverId AND r.hidden = false")
  long countByDriver(@Param("driverId") UUID driverId);
}
