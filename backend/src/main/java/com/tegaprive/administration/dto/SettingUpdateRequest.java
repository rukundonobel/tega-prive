package com.tegaprive.administration.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingUpdateRequest(@NotBlank String value) {
}
