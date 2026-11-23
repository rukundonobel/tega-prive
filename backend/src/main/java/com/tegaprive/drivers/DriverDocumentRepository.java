package com.tegaprive.drivers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverDocumentRepository extends JpaRepository<DriverDocument, UUID> {

  List<DriverDocument> findByDriverId(UUID driverId);
}
