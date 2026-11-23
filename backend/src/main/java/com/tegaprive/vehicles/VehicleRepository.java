package com.tegaprive.vehicles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

  List<Vehicle> findByDriverId(UUID driverId);

  boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

  List<Vehicle> findByApprovalStatus(VehicleApprovalStatus status);

  Optional<Vehicle> findByIdAndDriverId(UUID id, UUID driverId);
}
