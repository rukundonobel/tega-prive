package com.tegaprive.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for admin reject actions (driver verification / vehicle approval). */
public record RejectionRequest(@NotBlank @Size(max = 500) String reason) {
}
