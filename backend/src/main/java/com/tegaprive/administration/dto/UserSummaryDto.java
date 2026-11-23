package com.tegaprive.administration.dto;

import java.time.Instant;
import java.util.List;

public record UserSummaryDto(
    String id,
    String name,
    String email,
    String phone,
    List<String> roles,
    String status,
    Instant createdAt) {
}
