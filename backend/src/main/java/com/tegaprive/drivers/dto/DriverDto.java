package com.tegaprive.drivers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverDto(
    String id,
    String userId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String licenceNumber,
    LocalDate licenceExpiry,
    String profilePhotoUrl,
    String verificationStatus,
    boolean verified,
    BigDecimal ratingAvg,
    int ratingCount,
    String rejectionReason) {
}
