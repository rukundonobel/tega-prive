package com.tegaprive.bookings;

import com.tegaprive.bookings.dto.BookingDto;

public final class BookingMapper {

  private BookingMapper() {
  }

  public static BookingDto toDto(Booking booking) {
    var journey = booking.getJourney();
    var passenger = booking.getPassenger();
    return new BookingDto(
        booking.getId().toString(),
        journey.getId().toString(),
        passenger.getId().toString(),
        (passenger.getFirstName() + " " + passenger.getLastName()).trim(),
        passenger.getPhone(),
        journey.getOrigin().getName(),
        journey.getDestination().getName(),
        journey.getDepartureTime(),
        booking.getSeatsBooked(),
        booking.getStatus().name(),
        booking.getGrossFare(),
        booking.getServiceFee(),
        booking.getTotalAmount(),
        booking.getPaymentMethod(),
        booking.getExpiresAt(),
        booking.getConfirmedAt(),
        booking.getCancelledAt(),
        booking.getCancelReason(),
        booking.getCreatedAt());
  }
}
