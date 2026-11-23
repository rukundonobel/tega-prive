package com.tegaprive.vehicles;

import java.util.List;
import java.util.UUID;

import com.tegaprive.vehicles.dto.VehicleDocumentDto;
import com.tegaprive.vehicles.dto.VehicleDto;
import com.tegaprive.vehicles.dto.VehiclePhotoDto;

/** Maps Vehicle/Photo/Document entities to their public DTOs. */
public final class VehicleMapper {

  private VehicleMapper() {
  }

  public static VehicleDto toDto(Vehicle vehicle, UUID driverId, String driverName,
      List<VehiclePhoto> photos, List<VehicleDocument> documents) {
    return new VehicleDto(
        vehicle.getId().toString(),
        driverId.toString(),
        driverName,
        vehicle.getMake(),
        vehicle.getModel(),
        vehicle.getManufacturingYear(),
        vehicle.getColour(),
        vehicle.getRegistrationNumber(),
        vehicle.getCategory().name(),
        vehicle.getSeatCapacity(),
        vehicle.getLuggageCapacity(),
        vehicle.isHasAc(),
        vehicle.getDescription(),
        vehicle.getApprovalStatus().name(),
        vehicle.getRejectionReason(),
        photos.stream().map(VehicleMapper::toPhotoDto).toList(),
        documents.stream().map(VehicleMapper::toDocumentDto).toList());
  }

  public static VehiclePhotoDto toPhotoDto(VehiclePhoto photo) {
    return new VehiclePhotoDto(photo.getId().toString(), photo.getUrl(), photo.getPosition(), photo.isPrimary());
  }

  public static VehicleDocumentDto toDocumentDto(VehicleDocument document) {
    return new VehicleDocumentDto(document.getId().toString(), document.getType().name(),
        document.getUrl(), document.getStatus().name(), document.getRejectionReason());
  }
}
