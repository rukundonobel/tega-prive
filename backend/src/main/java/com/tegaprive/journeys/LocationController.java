package com.tegaprive.journeys;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.journeys.dto.LocationDto;
import com.tegaprive.journeys.dto.LocationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

  private final LocationService locationService;

  public LocationController(LocationService locationService) {
    this.locationService = locationService;
  }

  /** Public: list all active locations for the search dropdowns. */
  @GetMapping
  public ApiResponse<List<LocationDto>> list() {
    return ApiResponse.ok(locationService.listActive());
  }

  /** Admin: list all (including inactive). */
  @GetMapping("/all")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<LocationDto>> listAll() {
    return ApiResponse.ok(locationService.listAll());
  }

  /** Admin: add a new location. */
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LocationDto> create(@Valid @RequestBody LocationRequest request) {
    return ApiResponse.ok(locationService.create(request));
  }

  /** Admin: enable or disable a location. */
  @PatchMapping("/{id}/active")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LocationDto> setActive(@PathVariable UUID id, @RequestParam boolean active) {
    return ApiResponse.ok(locationService.toggle(id, active));
  }
}
