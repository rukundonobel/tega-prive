package com.tegaprive.drivers;

import com.tegaprive.drivers.dto.DriverDocumentDto;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.DriverSummaryDto;
import com.tegaprive.users.User;

/** Maps Driver/DriverDocument entities to their public DTOs. */
public final class DriverMapper {

  private DriverMapper() {
  }

  public static DriverDto toDto(Driver driver, User user) {
    return new DriverDto(
        driver.getId().toString(),
        user.getId().toString(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhone(),
        driver.getLicenceNumber(),
        driver.getLicenceExpiry(),
        driver.getProfilePhotoUrl(),
        driver.getVerificationStatus().name(),
        driver.isVerified(),
        driver.getRatingAvg(),
        driver.getRatingCount(),
        driver.getRejectionReason());
  }

  public static DriverSummaryDto toSummaryDto(Driver driver, User user) {
    return new DriverSummaryDto(
        driver.getId().toString(),
        user.getId().toString(),
        (user.getFirstName() + " " + user.getLastName()).trim(),
        user.getEmail(),
        driver.getLicenceNumber(),
        driver.getVerificationStatus().name(),
        driver.isVerified(),
        driver.getCreatedAt());
  }

  public static DriverDocumentDto toDocumentDto(DriverDocument doc) {
    return new DriverDocumentDto(
        doc.getId().toString(),
        doc.getType().name(),
        doc.getUrl(),
        doc.getStatus().name(),
        doc.getRejectionReason(),
        doc.getCreatedAt());
  }
}
