package com.tegaprive.commissions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverEarningRepository extends JpaRepository<DriverEarning, UUID> {

  List<DriverEarning> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

  Optional<DriverEarning> findByBookingId(UUID bookingId);

  @Query("SELECT COALESCE(SUM(e.netEarnings), 0) FROM DriverEarning e WHERE e.driver.id = :driverId AND e.settlementStatus = 'PENDING'")
  java.math.BigDecimal sumPendingEarnings(@Param("driverId") UUID driverId);
}
