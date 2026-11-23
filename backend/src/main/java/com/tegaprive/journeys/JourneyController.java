package com.tegaprive.journeys;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.tegaprive.journeys.dto.JourneyDto;
import com.tegaprive.journeys.dto.JourneyRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

  private final JourneyService journeyService;

  public JourneyController(JourneyService journeyService) {
    this.journeyService = journeyService;
  }

  /**
   * Public search — no authentication required.
   * GET /api/journeys/search?originId=&destinationId=&date=2024-12-25&seats=2
   */
  @GetMapping("/search")
  public ApiResponse<List<JourneyDto>> search(
      @RequestParam UUID originId,
      @RequestParam UUID destinationId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "1") int seats) {
    return ApiResponse.ok(journeyService.search(originId, destinationId, date, seats));
  }

  /** Public: single journey detail. */
  @GetMapping("/{id}")
  public ApiResponse<JourneyDto> get(@PathVariable UUID id) {
    return ApiResponse.ok(journeyService.get(id));
  }

  /** Driver: publish a new journey. */
  @PostMapping
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<JourneyDto> publish(
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody JourneyRequest request) {
    return ApiResponse.ok(journeyService.publish(userId, request));
  }

  /** Driver: list own journeys. */
  @GetMapping("/my")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<List<JourneyDto>> myJourneys(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(journeyService.myJourneys(userId));
  }

  /** Driver: start journey. */
  @PostMapping("/{id}/start")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<JourneyDto> start(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(journeyService.startJourney(userId, id));
  }

  /** Driver: complete journey. */
  @PostMapping("/{id}/complete")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<JourneyDto> complete(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(journeyService.completeJourney(userId, id));
  }

  /** Driver: cancel journey. */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<JourneyDto> cancel(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(journeyService.cancelJourney(userId, id));
  }
}
