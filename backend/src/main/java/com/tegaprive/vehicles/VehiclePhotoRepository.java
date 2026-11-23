package com.tegaprive.vehicles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, UUID> {

  List<VehiclePhoto> findByVehicleIdOrderByPositionAsc(UUID vehicleId);

  Optional<VehiclePhoto> findTopByVehicleIdOrderByPositionDesc(UUID vehicleId);
}
