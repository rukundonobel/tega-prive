package com.tegaprive.bookings;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;
import com.tegaprive.journeys.Journey;
import com.tegaprive.users.User;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "journey_id", nullable = false)
  private Journey journey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "passenger_id", nullable = false)
  private User passenger;

  @Column(name = "seats_booked", nullable = false)
  private int seatsBooked = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status = BookingStatus.PENDING_PAYMENT;

  /** Total amount the passenger pays (gross_fare + service_fee). */
  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  /** Platform service fee charged to the passenger. */
  @Column(name = "service_fee", nullable = false)
  private BigDecimal serviceFee = BigDecimal.ZERO;

  /** Raw fare before the service fee (pricePerSeat × seatsBooked). */
  @Column(name = "gross_fare", nullable = false)
  private BigDecimal grossFare;

  @Column(name = "payment_method")
  private String paymentMethod;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_reason")
  private String cancelReason;
}
