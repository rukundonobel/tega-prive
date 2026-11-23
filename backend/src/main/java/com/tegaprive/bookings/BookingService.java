package com.tegaprive.bookings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.dto.BookingDto;
import com.tegaprive.bookings.dto.BookingRequest;
import com.tegaprive.bookings.dto.CancelRequest;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.journeys.Journey;
import com.tegaprive.journeys.JourneyService;
import com.tegaprive.journeys.JourneyStatus;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;

/**
 * Booking workflow with overbooking prevention.
 *
 * Payment is now handled by PaymentService. When a payment is confirmed:
 *   PaymentService.advanceBookingOnSuccess() sets status=CONFIRMED here.
 * This service no longer decides payment outcomes.
 */
@Service
public class BookingService {

  /** Service fee percentage (3%). Kept here for booking creation; also computed in PaymentService. */
  private static final BigDecimal SERVICE_FEE_PCT = new BigDecimal("0.03");
  private static final int BOOKING_EXPIRY_MINUTES = 30;

  private final BookingRepository bookingRepository;
  private final JourneyService journeyService;
  private final UserRepository userRepository;

  public BookingService(BookingRepository bookingRepository, JourneyService journeyService,
      UserRepository userRepository) {
    this.bookingRepository = bookingRepository;
    this.journeyService = journeyService;
    this.userRepository = userRepository;
  }

  @Transactional
  @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
  public BookingDto createBooking(UUID userId, BookingRequest request) {
    User passenger = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Journey journey = journeyService.findById(request.journeyId());

    if (journey.getStatus() != JourneyStatus.SCHEDULED) {
      throw new BadRequestException("This journey is no longer accepting bookings");
    }
    if (journey.getAvailableSeats() < request.seatsRequested()) {
      throw new BadRequestException(
          "Not enough seats available. Requested: " + request.seatsRequested()
              + ", available: " + journey.getAvailableSeats());
    }

    bookingRepository.findActiveBooking(journey.getId(), passenger.getId())
        .ifPresent(b -> {
          throw new ConflictException("You already have an active booking for this journey");
        });

    journey.setAvailableSeats(journey.getAvailableSeats() - request.seatsRequested());

    // Amounts computed server-side.
    BigDecimal grossFare = journey.getPricePerSeat()
        .multiply(BigDecimal.valueOf(request.seatsRequested()))
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal serviceFee = grossFare.multiply(SERVICE_FEE_PCT).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalAmount = grossFare.add(serviceFee);

    Booking booking = new Booking();
    booking.setJourney(journey);
    booking.setPassenger(passenger);
    booking.setSeatsBooked(request.seatsRequested());
    booking.setStatus(BookingStatus.PENDING_PAYMENT);
    booking.setGrossFare(grossFare);
    booking.setServiceFee(serviceFee);
    booking.setTotalAmount(totalAmount);
    booking.setExpiresAt(Instant.now().plus(BOOKING_EXPIRY_MINUTES, ChronoUnit.MINUTES));

    bookingRepository.save(booking);
    return BookingMapper.toDto(booking);
  }

  /** Passenger cancels their booking. Returns seat to inventory. */
  @Transactional
  public BookingDto cancelByPassenger(UUID userId, UUID bookingId, CancelRequest request) {
    Booking booking = passengerBooking(userId, bookingId);
    if (booking.getStatus() == BookingStatus.CANCELLED
        || booking.getStatus() == BookingStatus.COMPLETED
        || booking.getStatus() == BookingStatus.REFUNDED) {
      throw new BadRequestException("Booking cannot be cancelled (status: " + booking.getStatus() + ")");
    }
    doCancel(booking, request.reason());
    return BookingMapper.toDto(booking);
  }

  /** Driver declines a pending booking. Returns seat. */
  @Transactional
  public BookingDto declineByDriver(UUID userId, UUID journeyId, UUID bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    if (!booking.getJourney().getId().equals(journeyId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    if (!booking.getJourney().getDriver().getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    doCancel(booking, "Declined by driver");
    return BookingMapper.toDto(booking);
  }

  @Transactional(readOnly = true)
  public List<BookingDto> myBookings(UUID userId) {
    return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(userId).stream()
        .map(BookingMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BookingDto> journeyBookings(UUID userId, UUID journeyId) {
    Journey journey = journeyService.findById(journeyId);
    if (!journey.getDriver().getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Journey not found");
    }
    return bookingRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId).stream()
        .map(BookingMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public BookingDto getMyBooking(UUID userId, UUID bookingId) {
    return BookingMapper.toDto(passengerBooking(userId, bookingId));
  }

  private void doCancel(Booking booking, String reason) {
    booking.setStatus(BookingStatus.CANCELLED);
    booking.setCancelledAt(Instant.now());
    booking.setCancelReason(reason);
    Journey journey = booking.getJourney();
    journey.setAvailableSeats(journey.getAvailableSeats() + booking.getSeatsBooked());
    bookingRepository.save(booking);
  }

  private Booking passengerBooking(UUID userId, UUID bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    if (!booking.getPassenger().getId().equals(userId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    return booking;
  }
}
