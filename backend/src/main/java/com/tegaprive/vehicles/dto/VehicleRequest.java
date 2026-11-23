package com.tegaprive.vehicles.dto;

import com.tegaprive.vehicles.VehicleCategory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
    @NotBlank @Size(max = 64) String make,
    @NotBlank @Size(max = 64) String model,
    @Min(1900) @Max(2100) Integer manufacturingYear,
    @NotBlank @Size(max = 32) String colour,
    @NotBlank @Size(max = 32) String registrationNumber,
    @NotNull VehicleCategory category,
    @Min(1) @Max(50) int seatCapacity,
    @Min(0) @Max(200) int luggageCapacity,
    boolean hasAc,
    @Size(max = 1000) String description) {
}
