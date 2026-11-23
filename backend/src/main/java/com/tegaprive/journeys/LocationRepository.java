package com.tegaprive.journeys;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, UUID> {

  List<Location> findByActiveTrue();

  Optional<Location> findByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);

  boolean existsByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);
}
