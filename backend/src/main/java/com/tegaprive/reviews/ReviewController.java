package com.tegaprive.reviews;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.reviews.dto.ReviewDto;
import com.tegaprive.reviews.dto.ReviewRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  /** Passenger: submit a review after completing a journey. */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<ReviewDto> submit(
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody ReviewRequest request) {
    return ApiResponse.ok(reviewService.submitReview(userId, request));
  }

  /** Public: reviews for a specific driver. */
  @GetMapping("/driver/{driverId}")
  public ApiResponse<List<ReviewDto>> driverReviews(@PathVariable UUID driverId) {
    return ApiResponse.ok(reviewService.driverReviews(driverId));
  }
}
