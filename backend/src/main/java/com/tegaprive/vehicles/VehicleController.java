package com.tegaprive.vehicles;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.vehicles.dto.VehicleDocumentDto;
import com.tegaprive.vehicles.dto.VehicleDto;
import com.tegaprive.vehicles.dto.VehiclePhotoDto;
import com.tegaprive.vehicles.dto.VehicleRequest;

import jakarta.validation.Valid;

/** Driver-facing vehicle endpoints. Ownership is enforced per request via the JWT principal. */
@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasRole('DRIVER')")
public class VehicleController {

  private final VehicleService vehicleService;

  public VehicleController(VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  @PostMapping
  public ApiResponse<VehicleDto> create(@AuthenticationPrincipal UUID userId,
      @Valid @RequestBody VehicleRequest request) {
    return ApiResponse.ok(vehicleService.createVehicle(userId, request));
  }

  @GetMapping
  public ApiResponse<List<VehicleDto>> list(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(vehicleService.listMyVehicles(userId));
  }

  @GetMapping("/{id}")
  public ApiResponse<VehicleDto> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(vehicleService.getMyVehicle(userId, id));
  }

  @PutMapping("/{id}")
  public ApiResponse<VehicleDto> update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
      @Valid @RequestBody VehicleRequest request) {
    return ApiResponse.ok(vehicleService.updateVehicle(userId, id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    vehicleService.deleteVehicle(userId, id);
    return ApiResponse.ok(null, "Vehicle deleted");
  }

  @PostMapping("/{id}/photos")
  public ApiResponse<VehiclePhotoDto> addPhoto(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(vehicleService.addPhoto(userId, id, file));
  }

  @DeleteMapping("/{id}/photos/{photoId}")
  public ApiResponse<Void> removePhoto(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
      @PathVariable UUID photoId) {
    vehicleService.removePhoto(userId, id, photoId);
    return ApiResponse.ok(null, "Photo removed");
  }

  @PostMapping("/{id}/documents")
  public ApiResponse<VehicleDocumentDto> addDocument(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
      @RequestParam("type") VehicleDocumentType type,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(vehicleService.addDocument(userId, id, type, file));
  }

  @PostMapping("/{id}/submit")
  public ApiResponse<VehicleDto> submit(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(vehicleService.submitForApproval(userId, id));
  }
}
