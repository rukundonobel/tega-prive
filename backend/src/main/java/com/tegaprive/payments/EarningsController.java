package com.tegaprive.payments;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.commissions.CommissionAdminService;
import com.tegaprive.commissions.dto.DriverEarningDto;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.DriverRepository;

/** Driver-facing earnings endpoints under /api/earnings. */
@RestController
@RequestMapping("/api/earnings")
@PreAuthorize("hasRole('DRIVER')")
public class EarningsController {

  private final CommissionAdminService adminService;
  private final DriverRepository driverRepository;

  public EarningsController(CommissionAdminService adminService, DriverRepository driverRepository) {
    this.adminService = adminService;
    this.driverRepository = driverRepository;
  }

  @GetMapping
  public ApiResponse<List<DriverEarningDto>> myEarnings(@AuthenticationPrincipal UUID userId) {
    var driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    return ApiResponse.ok(adminService.driverEarnings(driver.getId()));
  }

  @GetMapping("/summary")
  public ApiResponse<java.math.BigDecimal> pendingBalance(@AuthenticationPrincipal UUID userId) {
    var driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    return ApiResponse.ok(adminService.pendingEarnings(driver.getId()));
  }
}
