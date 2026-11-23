package com.tegaprive.journeys;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.journeys.dto.LocationDto;
import com.tegaprive.journeys.dto.LocationRequest;

@Service
public class LocationService {

  private final LocationRepository locationRepository;

  public LocationService(LocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  @Transactional(readOnly = true)
  public List<LocationDto> listActive() {
    return locationRepository.findByActiveTrue().stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LocationDto> listAll() {
    return locationRepository.findAll().stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public LocationDto create(LocationRequest request) {
    if (locationRepository.existsByNameIgnoreCaseAndCountryIgnoreCase(
        request.name(), request.country())) {
      throw new ConflictException("Location already exists: " + request.name());
    }
    Location loc = new Location();
    loc.setName(request.name());
    loc.setRegion(request.region());
    loc.setCountry(request.country());
    loc.setActive(true);
    locationRepository.save(loc);
    return toDto(loc);
  }

  @Transactional
  public LocationDto toggle(UUID id, boolean active) {
    Location loc = locationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    loc.setActive(active);
    locationRepository.save(loc);
    return toDto(loc);
  }

  public Location findById(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
  }

  private LocationDto toDto(Location l) {
    return new LocationDto(l.getId().toString(), l.getName(), l.getRegion(), l.getCountry(), l.isActive());
  }
}
