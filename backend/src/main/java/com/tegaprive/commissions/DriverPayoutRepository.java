package com.tegaprive.commissions;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverPayoutRepository extends JpaRepository<DriverPayout, UUID> {

  List<DriverPayout> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

  List<DriverPayout> findByStatus(String status);
}
