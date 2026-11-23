package com.tegaprive.reviews;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.Booking;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.reviews.dto.ReviewDto;
import com.tegaprive.reviews.dto.ReviewRequest;
import com.tegaprive.users.UserRepository;

@Service
public class ReviewService {

  private final ReviewRepository reviewRepo;
  private final BookingRepository bookingRepo;
  private final DriverRepository driverRepo;
  private final UserRepository userRepo;

  public ReviewService(ReviewRepository reviewRepo, BookingRepository bookingRepo,
      DriverRepository driverRepo, UserRepository userRepo) {
    this.reviewRepo = reviewRepo;
    this.bookingRepo = bookingRepo;
    this.driverRepo = driverRepo;
    this.userRepo = userRepo;
  }

  @Transactional
  public ReviewDto submitReview(UUID userId, ReviewRequest request) {
    Booking booking = bookingRepo.findById(request.bookingId())
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    if (!booking.getPassenger().getId().equals(userId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    if (booking.getStatus() != BookingStatus.COMPLETED) {
      throw new BadRequestException("Reviews can only be submitted for completed journeys");
    }
    if (reviewRepo.findByBookingId(booking.getId()).isPresent()) {
      throw new ConflictException("A review for this booking already exists");
    }

    Driver driver = booking.getJourney().getDriver();

    Review review = new Review();
    review.setBooking(booking);
    review.setReviewer(booking.getPassenger());
    review.setDriver(driver);
    review.setRating(request.rating());
    review.setComment(request.comment());
    reviewRepo.save(review);

    // Update driver average rating
    Double avg = reviewRepo.averageRatingByDriver(driver.getId());
    long count = reviewRepo.countByDriver(driver.getId());
    if (avg != null) {
      driver.setRatingAvg(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
      driver.setRatingCount((int) count);
      driverRepo.save(driver);
    }

    return toDto(review);
  }

  @Transactional(readOnly = true)
  public List<ReviewDto> driverReviews(UUID driverId) {
    return reviewRepo.findByDriverId(driverId).stream()
        .filter(r -> !r.isHidden())
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewDto> allReviews() {
    return reviewRepo.findAll().stream().map(this::toDto).toList();
  }

  @Transactional
  public ReviewDto moderate(UUID reviewId, boolean hide) {
    Review review = reviewRepo.findById(reviewId)
        .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    review.setHidden(hide);
    review.setModerated(true);
    reviewRepo.save(review);
    return toDto(review);
  }

  private ReviewDto toDto(Review r) {
    return new ReviewDto(
        r.getId().toString(),
        r.getBooking().getId().toString(),
        r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName(),
        r.getDriver().getId().toString(),
        r.getDriver().getUser().getFirstName() + " " + r.getDriver().getUser().getLastName(),
        r.getRating(),
        r.getComment(),
        r.isHidden(),
        r.getCreatedAt());
  }
}
