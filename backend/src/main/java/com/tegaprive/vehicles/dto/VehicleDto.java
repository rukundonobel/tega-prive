package com.tegaprive.vehicles.dto;

import java.util.List;

public record VehicleDto(
    String id,
    String driverId,
    String driverName,
    String make,
    String model,
    Integer manufacturingYear,
    String colour,
    String registrationNumber,
    String category,
    int seatCapacity,
    int luggageCapacity,
    boolean hasAc,
    String description,
    String approvalStatus,
    String rejectionReason,
    List<VehiclePhotoDto> photos,
    List<VehicleDocumentDto> documents) {
}
