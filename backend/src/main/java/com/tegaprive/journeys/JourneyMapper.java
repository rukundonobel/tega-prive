package com.tegaprive.journeys;

import com.tegaprive.journeys.dto.JourneyDto;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehiclePhoto;
import com.tegaprive.vehicles.VehiclePhotoRepository;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JourneyMapper {

  private final VehiclePhotoRepository photoRepository;

  public JourneyMapper(VehiclePhotoRepository photoRepository) {
    this.photoRepository = photoRepository;
  }

  public JourneyDto toDto(Journey journey) {
    var driver = journey.getDriver();
    var user = driver.getUser();
    var vehicle = journey.getVehicle();

    String primaryPhotoUrl = photoRepository
        .findByVehicleIdOrderByPositionAsc(vehicle.getId())
        .stream().filter(VehiclePhoto::isPrimary).findFirst()
        .or(() -> photoRepository.findByVehicleIdOrderByPositionAsc(vehicle.getId()).stream().findFirst())
        .map(VehiclePhoto::getUrl)
        .orElse(null);

    return new JourneyDto(
        journey.getId().toString(),
        driver.getId().toString(),
        (user.getFirstName() + " " + user.getLastName()).trim(),
        driver.getProfilePhotoUrl(),
        driver.isVerified(),
        driver.getRatingAvg().doubleValue(),
        driver.getRatingCount(),
        vehicle.getId().toString(),
        vehicle.getMake(),
        vehicle.getModel(),
        vehicle.getCategory().name(),
        vehicle.getSeatCapacity(),
        vehicle.isHasAc(),
        primaryPhotoUrl,
        journey.getOrigin().getId().toString(),
        journey.getOrigin().getName(),
        journey.getDestination().getId().toString(),
        journey.getDestination().getName(),
        journey.getDepartureTime(),
        journey.getEstimatedArrival(),
        journey.getPricePerSeat(),
        journey.getTotalSeats(),
        journey.getAvailableSeats(),
        journey.getStatus().name(),
        journey.getPickupInstructions(),
        journey.getDropoffInstructions(),
        journey.getNotes(),
        journey.getCreatedAt());
  }
}
