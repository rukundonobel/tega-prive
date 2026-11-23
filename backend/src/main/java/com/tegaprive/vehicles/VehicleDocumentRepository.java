package com.tegaprive.vehicles;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

  List<VehicleDocument> findByVehicleId(UUID vehicleId);

  boolean existsByVehicleIdAndType(UUID vehicleId, VehicleDocumentType type);
}
