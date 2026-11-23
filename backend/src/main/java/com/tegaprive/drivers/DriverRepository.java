package com.tegaprive.drivers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

  Optional<Driver> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);

  List<Driver> findByVerificationStatus(VerificationStatus status);
}
