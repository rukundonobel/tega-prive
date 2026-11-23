package com.tegaprive.administration;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.tegaprive.administration.dto.*;
import com.tegaprive.common.ApiResponse;
import com.tegaprive.common.RejectionRequest;
import com.tegaprive.complaints.ComplaintService;
import com.tegaprive.complaints.dto.ComplaintDto;
import com.tegaprive.complaints.dto.ResolveRequest;
import com.tegaprive.drivers.DriverService;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.DriverSummaryDto;
import com.tegaprive.payments.PlatformSetting;
import com.tegaprive.payments.PlatformSettingsRepository;
import com.tegaprive.reviews.ReviewService;
import com.tegaprive.reviews.dto.ReviewDto;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.vehicles.VehicleService;
import com.tegaprive.vehicles.dto.VehicleDto;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.dto.BookingDto;
import com.tegaprive.bookings.BookingMapper;

import jakarta.validation.Valid;
import java.time.Instant;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

  private final DriverService driverService;
  private final VehicleService vehicleService;
  private final ReportingService reportingService;
  private final ComplaintService complaintService;
  private final ReviewService reviewService;
  private final UserRepository userRepository;
  private final BookingRepository bookingRepository;
  private final PlatformSettingsRepository settingsRepository;

  public AdminController(DriverService driverService, VehicleService vehicleService,
      ReportingService reportingService, ComplaintService complaintService,
      ReviewService reviewService, UserRepository userRepository,
      BookingRepository bookingRepository, PlatformSettingsRepository settingsRepository) {
    this.driverService = driverService;
    this.vehicleService = vehicleService;
    this.reportingService = reportingService;
    this.complaintService = complaintService;
    this.reviewService = reviewService;
    this.userRepository = userRepository;
    this.bookingRepository = bookingRepository;
    this.settingsRepository = settingsRepository;
  }

  // ── OVERVIEW ──────────────────────────────────────────────────────────────

  @GetMapping("/stats")
  public ApiResponse<DashboardStatsDto> stats() {
    return ApiResponse.ok(reportingService.getDashboardStats());
  }

  @GetMapping("/bookings/recent")
  public ApiResponse<List<BookingSummaryDto>> recentBookings(
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.ok(reportingService.recentBookings(limit));
  }

  @GetMapping("/journeys/active")
  public ApiResponse<List<ActiveJourneyDto>> activeJourneys() {
    return ApiResponse.ok(reportingService.activeJourneys());
  }

  // ── REVENUE REPORTS ───────────────────────────────────────────────────────

  @GetMapping("/reports/revenue")
  public ApiResponse<RevenueReportDto> revenueReport(
      @RequestParam(defaultValue = "30") int days) {
    return ApiResponse.ok(reportingService.revenueReport(days));
  }

  // ── DRIVER VERIFICATION ───────────────────────────────────────────────────

  @GetMapping("/drivers")
  public ApiResponse<List<DriverSummaryDto>> pendingDrivers() {
    return ApiResponse.ok(driverService.listPendingDrivers());
  }

  @GetMapping("/drivers/all")
  public ApiResponse<List<DriverSummaryDto>> allDrivers() {
    return ApiResponse.ok(driverService.listAllDrivers());
  }

  @PostMapping("/drivers/{id}/approve")
  public ApiResponse<DriverDto> approveDriver(@PathVariable UUID id,
      @AuthenticationPrincipal UUID adminId) {
    return ApiResponse.ok(driverService.approveDriver(id, adminId));
  }

  @PostMapping("/drivers/{id}/reject")
  public ApiResponse<DriverDto> rejectDriver(@PathVariable UUID id,
      @AuthenticationPrincipal UUID adminId,
      @Valid @RequestBody RejectionRequest request) {
    return ApiResponse.ok(driverService.rejectDriver(id, request.reason(), adminId));
  }

  // ── VEHICLE APPROVAL ──────────────────────────────────────────────────────

  @GetMapping("/vehicles")
  public ApiResponse<List<VehicleDto>> pendingVehicles() {
    return ApiResponse.ok(vehicleService.listPendingVehicles());
  }

  @GetMapping("/vehicles/{id}")
  public ApiResponse<VehicleDto> getVehicle(@PathVariable UUID id) {
    return ApiResponse.ok(vehicleService.getVehicleForAdmin(id));
  }

  @PostMapping("/vehicles/{id}/approve")
  public ApiResponse<VehicleDto> approveVehicle(@PathVariable UUID id,
      @AuthenticationPrincipal UUID adminId) {
    return ApiResponse.ok(vehicleService.approveVehicle(id, adminId));
  }

  @PostMapping("/vehicles/{id}/reject")
  public ApiResponse<VehicleDto> rejectVehicle(@PathVariable UUID id,
      @AuthenticationPrincipal UUID adminId,
      @Valid @RequestBody RejectionRequest request) {
    return ApiResponse.ok(vehicleService.rejectVehicle(id, request.reason(), adminId));
  }

  // ── USER MANAGEMENT ───────────────────────────────────────────────────────

  @GetMapping("/users")
  public ApiResponse<List<UserSummaryDto>> allUsers() {
    return ApiResponse.ok(userRepository.findAll().stream()
        .map(u -> new UserSummaryDto(
            u.getId().toString(),
            u.getFirstName() + " " + u.getLastName(),
            u.getEmail(),
            u.getPhone(),
            u.getRoles().stream().map(Enum::name).toList(),
            u.getStatus().name(),
            u.getCreatedAt()))
        .toList());
  }

  @PostMapping("/users/{id}/suspend")
  public ApiResponse<UserSummaryDto> suspend(@PathVariable UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new com.tegaprive.common.exception.ResourceNotFoundException("User not found"));
    user.setStatus(com.tegaprive.users.UserStatus.SUSPENDED);
    userRepository.save(user);
    return ApiResponse.ok(toUserSummary(user));
  }

  @PostMapping("/users/{id}/activate")
  public ApiResponse<UserSummaryDto> activate(@PathVariable UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new com.tegaprive.common.exception.ResourceNotFoundException("User not found"));
    user.setStatus(com.tegaprive.users.UserStatus.ACTIVE);
    userRepository.save(user);
    return ApiResponse.ok(toUserSummary(user));
  }

  // ── BOOKING MANAGEMENT ────────────────────────────────────────────────────

  @GetMapping("/bookings")
  public ApiResponse<List<BookingDto>> allBookings() {
    return ApiResponse.ok(bookingRepository.findAll().stream()
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .map(BookingMapper::toDto)
        .toList());
  }

  // ── COMPLAINTS ────────────────────────────────────────────────────────────

  @GetMapping("/complaints")
  public ApiResponse<List<ComplaintDto>> complaints() {
    return ApiResponse.ok(complaintService.allComplaints());
  }

  @GetMapping("/complaints/open")
  public ApiResponse<List<ComplaintDto>> openComplaints() {
    return ApiResponse.ok(complaintService.openComplaints());
  }

  @PostMapping("/complaints/{id}/resolve")
  public ApiResponse<ComplaintDto> resolveComplaint(@PathVariable UUID id,
      @AuthenticationPrincipal UUID adminId,
      @Valid @RequestBody ResolveRequest request) {
    return ApiResponse.ok(complaintService.resolve(id, request, adminId));
  }

  // ── REVIEWS ───────────────────────────────────────────────────────────────

  @GetMapping("/reviews")
  public ApiResponse<List<ReviewDto>> allReviews() {
    return ApiResponse.ok(reviewService.allReviews());
  }

  @PostMapping("/reviews/{id}/hide")
  public ApiResponse<ReviewDto> hideReview(@PathVariable UUID id) {
    return ApiResponse.ok(reviewService.moderate(id, true));
  }

  @PostMapping("/reviews/{id}/show")
  public ApiResponse<ReviewDto> showReview(@PathVariable UUID id) {
    return ApiResponse.ok(reviewService.moderate(id, false));
  }

  // ── PLATFORM SETTINGS ─────────────────────────────────────────────────────

  @GetMapping("/settings")
  public ApiResponse<List<SettingDto>> settings() {
    return ApiResponse.ok(settingsRepository.findAll().stream()
        .map(s -> new SettingDto(s.getKey(), s.getValue(), s.getDescription()))
        .toList());
  }

  @PutMapping("/settings/{key}")
  public ApiResponse<SettingDto> updateSetting(@PathVariable String key,
      @RequestBody SettingUpdateRequest request) {
    PlatformSetting setting = settingsRepository.findById(key)
        .orElseGet(() -> {
          PlatformSetting s = new PlatformSetting();
          s.setKey(key);
          return s;
        });
    setting.setValue(request.value());
    setting.setUpdatedAt(Instant.now());
    settingsRepository.save(setting);
    return ApiResponse.ok(new SettingDto(setting.getKey(), setting.getValue(), setting.getDescription()));
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private UserSummaryDto toUserSummary(User u) {
    return new UserSummaryDto(
        u.getId().toString(),
        u.getFirstName() + " " + u.getLastName(),
        u.getEmail(),
        u.getPhone(),
        u.getRoles().stream().map(Enum::name).toList(),
        u.getStatus().name(),
        u.getCreatedAt());
  }
}
