package com.tegaprive.drivers.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(
    @NotBlank @Size(max = 64) String licenceNumber,
    LocalDate licenceExpiry) {
}
