package com.tegaprive.drivers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.drivers.dto.DriverDocumentDto;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.RegisterDriverRequest;
import com.tegaprive.drivers.dto.UpdateDriverRequest;

import jakarta.validation.Valid;

/** Driver-facing endpoints: register, profile, photo and verification documents. */
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

  private final DriverService driverService;

  public DriverController(DriverService driverService) {
    this.driverService = driverService;
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<DriverDto> register(@AuthenticationPrincipal UUID userId,
      @Valid @RequestBody RegisterDriverRequest request) {
    return ApiResponse.ok(driverService.registerAsDriver(userId, request));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<DriverDto> me(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(driverService.getMyDriver(userId));
  }

  @PutMapping("/me")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<DriverDto> update(@AuthenticationPrincipal UUID userId,
      @Valid @RequestBody UpdateDriverRequest request) {
    return ApiResponse.ok(driverService.updateMyDriver(userId, request));
  }

  @PostMapping("/me/photo")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<DriverDto> uploadPhoto(@AuthenticationPrincipal UUID userId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(driverService.uploadProfilePhoto(userId, file));
  }

  @PostMapping("/me/documents")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<DriverDocumentDto> addDocument(@AuthenticationPrincipal UUID userId,
      @RequestParam("type") DriverDocumentType type,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(driverService.addDocument(userId, type, file));
  }

  @GetMapping("/me/documents")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<List<DriverDocumentDto>> documents(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(driverService.listMyDocuments(userId));
  }
}
