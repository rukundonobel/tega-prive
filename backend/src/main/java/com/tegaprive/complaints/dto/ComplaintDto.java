package com.tegaprive.complaints.dto;

import java.time.Instant;

public record ComplaintDto(
    String id,
    String complainantName,
    String complainantEmail,
    String subjectType,
    String subjectId,
    String category,
    String description,
    String status,
    String resolution,
    Instant createdAt) {
}
