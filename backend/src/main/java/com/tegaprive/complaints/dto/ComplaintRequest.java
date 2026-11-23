package com.tegaprive.complaints.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
    @NotBlank String subjectType,
    @NotNull UUID subjectId,
    @NotBlank String category,
    @NotBlank @Size(max = 2000) String description) {
}
