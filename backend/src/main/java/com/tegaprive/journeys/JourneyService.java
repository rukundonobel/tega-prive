package com.tegaprive.journeys;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.journeys.dto.JourneyDto;
import com.tegaprive.journeys.dto.JourneyRequest;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.vehicles.VehicleRepository;

@Service
public class JourneyService {

  private final JourneyRepository journeyRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final LocationService locationService;
  private final JourneyMapper mapper;

  public JourneyService(JourneyRepository journeyRepository, DriverRepository driverRepository,
      VehicleRepository vehicleRepository, LocationService locationService, JourneyMapper mapper) {
    this.journeyRepository = journeyRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.locationService = locationService;
    this.mapper = mapper;
  }

  /** Publish a new journey. Only allowed for verified drivers with an approved vehicle. */
  @Transactional
  public JourneyDto publish(UUID userId, JourneyRequest request) {
    Driver driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new BadRequestException("You must have a driver profile to publish journeys"));
    if (!driver.isVerified()) {
      throw new BadRequestException("Your driver profile must be verified before publishing journeys");
    }

    Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    if (!vehicle.getDriver().getId().equals(driver.getId())) {
      throw new ResourceNotFoundException("Vehicle not found");
    }
    if (vehicle.getApprovalStatus() != VehicleApprovalStatus.APPROVED) {
      throw new BadRequestException("Only approved vehicles can be used to publish journeys");
    }
    if (request.originId().equals(request.destinationId())) {
      throw new BadRequestException("Origin and destination must be different");
    }

    Location origin = locationService.findById(request.originId());
    Location destination = locationService.findById(request.destinationId());

    Journey journey = new Journey();
    journey.setDriver(driver);
    journey.setVehicle(vehicle);
    journey.setOrigin(origin);
    journey.setDestination(destination);
    journey.setDepartureTime(request.departureTime());
    journey.setEstimatedArrival(request.estimatedArrival());
    journey.setPricePerSeat(request.pricePerSeat());
    journey.setTotalSeats(request.totalSeats());
    journey.setAvailableSeats(request.totalSeats());
    journey.setStatus(JourneyStatus.SCHEDULED);
    journey.setPickupInstructions(request.pickupInstructions());
    journey.setDropoffInstructions(request.dropoffInstructions());
    journey.setNotes(request.notes());
    journeyRepository.save(journey);
    return mapper.toDto(journey);
  }

  /** All journeys published by the authenticated driver. */
  @Transactional(readOnly = true)
  public List<JourneyDto> myJourneys(UUID userId) {
    Driver driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    return journeyRepository.findByDriverId(driver.getId()).stream()
        .map(mapper::toDto)
        .toList();
  }

  /** Public search — no auth required. */
  @Transactional(readOnly = true)
  public List<JourneyDto> search(UUID originId, UUID destinationId, LocalDate date, int seats) {
    Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return journeyRepository.search(originId, destinationId, from, to, seats).stream()
        .map(mapper::toDto)
        .toList();
  }

  /** Public journey detail. */
  @Transactional(readOnly = true)
  public JourneyDto get(UUID journeyId) {
    return mapper.toDto(findById(journeyId));
  }

  /** Driver starts an active journey. */
  @Transactional
  public JourneyDto startJourney(UUID userId, UUID journeyId) {
    Journey journey = ownedJourney(userId, journeyId);
    if (journey.getStatus() != JourneyStatus.SCHEDULED) {
      throw new BadRequestException("Journey cannot be started (status: " + journey.getStatus() + ")");
    }
    journey.setStatus(JourneyStatus.ACTIVE);
    journeyRepository.save(journey);
    return mapper.toDto(journey);
  }

  /** Driver completes a journey. */
  @Transactional
  public JourneyDto completeJourney(UUID userId, UUID journeyId) {
    Journey journey = ownedJourney(userId, journeyId);
    if (journey.getStatus() != JourneyStatus.ACTIVE) {
      throw new BadRequestException("Journey is not active");
    }
    journey.setStatus(JourneyStatus.COMPLETED);
    journeyRepository.save(journey);
    return mapper.toDto(journey);
  }

  /** Driver cancels a journey. */
  @Transactional
  public JourneyDto cancelJourney(UUID userId, UUID journeyId) {
    Journey journey = ownedJourney(userId, journeyId);
    if (journey.getStatus() == JourneyStatus.COMPLETED) {
      throw new BadRequestException("A completed journey cannot be cancelled");
    }
    journey.setStatus(JourneyStatus.CANCELLED);
    journeyRepository.save(journey);
    return mapper.toDto(journey);
  }

  public Journey findById(UUID id) {
    return journeyRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Journey not found"));
  }

  private Journey ownedJourney(UUID userId, UUID journeyId) {
    Driver driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    Journey journey = findById(journeyId);
    if (!journey.getDriver().getId().equals(driver.getId())) {
      throw new ResourceNotFoundException("Journey not found");
    }
    return journey;
  }
}
