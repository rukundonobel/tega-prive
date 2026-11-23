package com.tegaprive.commissions;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.commissions.dto.CommissionRuleDto;
import com.tegaprive.commissions.dto.CommissionRuleRequest;
import com.tegaprive.commissions.dto.DriverEarningDto;
import com.tegaprive.commissions.dto.DriverPayoutDto;
import com.tegaprive.commissions.dto.PayoutRequest;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/commissions")
public class CommissionController {

  private final CommissionAdminService adminService;
  private final DriverEarningRepository earningRepository;
  private final DriverRepository driverRepository;

  public CommissionController(CommissionAdminService adminService,
      DriverEarningRepository earningRepository,
      DriverRepository driverRepository) {
    this.adminService = adminService;
    this.earningRepository = earningRepository;
    this.driverRepository = driverRepository;
  }

  // ── Admin: commission rules ──

  @GetMapping("/rules/current")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<CommissionRuleDto> currentRule() {
    return ApiResponse.ok(adminService.currentRule());
  }

  @GetMapping("/rules")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<List<CommissionRuleDto>> ruleHistory() {
    return ApiResponse.ok(adminService.ruleHistory());
  }

  @PostMapping("/rules")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<CommissionRuleDto> createRule(
      @AuthenticationPrincipal UUID adminId,
      @Valid @RequestBody CommissionRuleRequest request) {
    return ApiResponse.ok(adminService.createRule(request, adminId));
  }

  // ── Admin: driver earnings ──

  @GetMapping("/earnings/driver/{driverId}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<List<DriverEarningDto>> driverEarnings(@PathVariable UUID driverId) {
    return ApiResponse.ok(adminService.driverEarnings(driverId));
  }

  @GetMapping("/earnings/pending/{driverId}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<java.math.BigDecimal> pendingEarnings(@PathVariable UUID driverId) {
    return ApiResponse.ok(adminService.pendingEarnings(driverId));
  }

  // ── Driver: own earnings ──

  @GetMapping("/earnings/my")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<List<DriverEarningDto>> myEarnings(@AuthenticationPrincipal UUID userId) {
    var driver = driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    return ApiResponse.ok(adminService.driverEarnings(driver.getId()));
  }

  // ── Admin: payouts ──

  @GetMapping("/payouts/pending")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<List<DriverPayoutDto>> pendingPayouts() {
    return ApiResponse.ok(adminService.pendingPayouts());
  }

  @PostMapping("/payouts")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<DriverPayoutDto> initiatePayout(
      @AuthenticationPrincipal UUID adminId,
      @Valid @RequestBody PayoutRequest request) {
    return ApiResponse.ok(adminService.initiateManualPayout(request, adminId));
  }

  @PostMapping("/payouts/{id}/paid")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<DriverPayoutDto> markPaid(
      @AuthenticationPrincipal UUID adminId,
      @PathVariable UUID id,
      @RequestParam(required = false, defaultValue = "") String providerReference) {
    return ApiResponse.ok(adminService.markPayoutPaid(id, providerReference, adminId));
  }
}
