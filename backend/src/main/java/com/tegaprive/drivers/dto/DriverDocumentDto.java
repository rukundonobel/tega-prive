package com.tegaprive.drivers.dto;

import java.time.Instant;

public record DriverDocumentDto(
    String id,
    String type,
    String url,
    String status,
    String rejectionReason,
    Instant createdAt) {
}
