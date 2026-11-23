package com.tegaprive.journeys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 128) String region,
    @NotBlank @Size(max = 64) String country) {
}
