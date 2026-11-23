package com.tegaprive.bookings;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.bookings.dto.BookingDto;
import com.tegaprive.bookings.dto.BookingRequest;
import com.tegaprive.bookings.dto.CancelRequest;
import com.tegaprive.common.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  /** Passenger: create a booking (status = PENDING_PAYMENT). */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<BookingDto> create(
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody BookingRequest request) {
    return ApiResponse.ok(bookingService.createBooking(userId, request));
  }

  /** Passenger: list own bookings. */
  @GetMapping("/my")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<BookingDto>> myBookings(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(bookingService.myBookings(userId));
  }

  /** Passenger: single booking detail. */
  @GetMapping("/my/{id}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<BookingDto> myBooking(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
    return ApiResponse.ok(bookingService.getMyBooking(userId, id));
  }

  /** Passenger: cancel a booking. Returns seats to inventory. */
  @PostMapping("/my/{id}/cancel")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<BookingDto> cancel(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID id,
      @Valid @RequestBody CancelRequest request) {
    return ApiResponse.ok(bookingService.cancelByPassenger(userId, id, request));
  }

  /** Driver: list bookings on one of their journeys. */
  @GetMapping("/journey/{journeyId}")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<List<BookingDto>> journeyBookings(
      @AuthenticationPrincipal UUID userId, @PathVariable UUID journeyId) {
    return ApiResponse.ok(bookingService.journeyBookings(userId, journeyId));
  }

  /** Driver: decline a booking on one of their journeys. */
  @PostMapping("/journey/{journeyId}/{bookingId}/decline")
  @PreAuthorize("hasRole('DRIVER')")
  public ApiResponse<BookingDto> decline(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID journeyId,
      @PathVariable UUID bookingId) {
    return ApiResponse.ok(bookingService.declineByDriver(userId, journeyId, bookingId));
  }
}
