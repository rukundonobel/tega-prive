package com.tegaprive.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.Booking;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.commissions.CommissionRule;
import com.tegaprive.commissions.CommissionRuleRepository;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.drivers.VerificationStatus;
import com.tegaprive.journeys.Journey;
import com.tegaprive.journeys.JourneyRepository;
import com.tegaprive.journeys.JourneyStatus;
import com.tegaprive.journeys.Location;
import com.tegaprive.journeys.LocationRepository;
import com.tegaprive.payments.dto.InitiatePaymentRequest;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.vehicles.VehicleCategory;
import com.tegaprive.vehicles.VehicleRepository;

/**
 * Tests the payment service: initiation, dev-confirm bypass, duplicate prevention,
 * booking status advancement after confirmation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

  private static final AtomicInteger SEQ = new AtomicInteger(800);

  @Autowired PaymentService paymentService;
  @Autowired PaymentRepository paymentRepository;
  @Autowired BookingRepository bookingRepository;
  @Autowired UserRepository userRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired VehicleRepository vehicleRepository;
  @Autowired JourneyRepository journeyRepository;
  @Autowired LocationRepository locationRepository;
  @Autowired CommissionRuleRepository ruleRepository;

  private Booking pendingBooking;
  private UUID passengerUserId;

  @BeforeEach
  void setup() {
    ensureRule();

    User driverUser = user("pst-drv-" + SEQ.incrementAndGet() + "@test.rw");
    Driver driver = new Driver();
    driver.setUser(driverUser);
    driver.setVerified(true);
    driver.setVerificationStatus(VerificationStatus.APPROVED);
    driver.setRatingAvg(BigDecimal.ZERO);
    driver.setCommissionWalletBalance(BigDecimal.ZERO);
    driverRepository.save(driver);

    Vehicle vehicle = new Vehicle();
    vehicle.setDriver(driver);
    vehicle.setMake("Toyota"); vehicle.setModel("Corolla");
    vehicle.setColour("Silver");
    vehicle.setRegistrationNumber("PST-" + SEQ.incrementAndGet());
    vehicle.setCategory(VehicleCategory.SEDAN);
    vehicle.setSeatCapacity(4); vehicle.setLuggageCapacity(2);
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicleRepository.save(vehicle);

    Location origin = loc("PST-KGL-" + SEQ.incrementAndGet());
    Location dest   = loc("PST-HYE-" + SEQ.incrementAndGet());

    Journey journey = new Journey();
    journey.setDriver(driver); journey.setVehicle(vehicle);
    journey.setOrigin(origin); journey.setDestination(dest);
    journey.setDepartureTime(Instant.now().plus(3, ChronoUnit.HOURS));
    journey.setPricePerSeat(new BigDecimal("5000"));
    journey.setTotalSeats(4); journey.setAvailableSeats(2);
    journey.setStatus(JourneyStatus.SCHEDULED);
    journeyRepository.save(journey);

    User passenger = user("pst-pas-" + SEQ.incrementAndGet() + "@test.rw");
    passengerUserId = passenger.getId();

    Booking booking = new Booking();
    booking.setJourney(journey);
    booking.setPassenger(passenger);
    booking.setSeatsBooked(1);
    booking.setStatus(BookingStatus.PENDING_PAYMENT);
    booking.setGrossFare(new BigDecimal("5000.00"));
    booking.setServiceFee(new BigDecimal("150.00"));
    booking.setTotalAmount(new BigDecimal("5150.00"));
    booking.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
    bookingRepository.save(booking);
    pendingBooking = booking;
  }

  @Test
  void initiateCreatesPaymentRecord() {
    // MTN MoMo not configured in test → expect "not configured" exception
    assertThatThrownBy(() ->
        paymentService.initiate(passengerUserId,
            new InitiatePaymentRequest(pendingBooking.getId(), PaymentProvider.MTN_MOMO, "+250788000111")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not configured");
  }

  @Test
  void devConfirmAdvancesBookingToConfirmed() {
    // Create a pending payment directly (bypasses provider)
    Payment payment = new Payment();
    payment.setBooking(pendingBooking);
    payment.setProvider(PaymentProvider.MTN_MOMO);
    payment.setInternalReference("TP-DEV-" + SEQ.incrementAndGet());
    payment.setAmount(new BigDecimal("5150.00"));
    payment.setCurrency("RWF");
    payment.setPhoneNumber("+250788000222");
    payment.setStatus(PaymentStatus.PENDING);
    payment.setVerified(false);
    paymentRepository.save(payment);

    var dto = paymentService.devConfirm(passengerUserId, payment.getId());

    assertThat(dto.status()).isEqualTo("SUCCESSFUL");
    assertThat(dto.verified()).isTrue();

    // Booking must be CONFIRMED
    var booking = bookingRepository.findById(pendingBooking.getId()).orElseThrow();
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
  }

  @Test
  void devConfirmIsIdempotent() {
    Payment payment = new Payment();
    payment.setBooking(pendingBooking);
    payment.setProvider(PaymentProvider.AIRTEL);
    payment.setInternalReference("TP-DEV-" + SEQ.incrementAndGet());
    payment.setAmount(new BigDecimal("5150.00"));
    payment.setCurrency("RWF");
    payment.setPhoneNumber("+250788000333");
    payment.setStatus(PaymentStatus.PENDING);
    paymentRepository.save(payment);

    paymentService.devConfirm(passengerUserId, payment.getId());
    var dto = paymentService.devConfirm(passengerUserId, payment.getId()); // second call

    assertThat(dto.status()).isEqualTo("SUCCESSFUL");
  }

  @Test
  void webhookAdvancesPaymentOnSuccess() throws Exception {
    Payment payment = new Payment();
    payment.setBooking(pendingBooking);
    payment.setProvider(PaymentProvider.MTN_MOMO);
    String ref = "TP-WH-" + SEQ.incrementAndGet();
    payment.setInternalReference(ref);
    payment.setAmount(new BigDecimal("5150.00"));
    payment.setCurrency("RWF");
    payment.setPhoneNumber("+250788000444");
    payment.setStatus(PaymentStatus.PENDING);
    paymentRepository.save(payment);

    String rawPayload = """
        {"internalReference":"%s","providerReference":"MOMO-REF-123","status":"SUCCESSFUL","failureReason":null}
        """.formatted(ref);

    paymentService.handleWebhook("MTN_MOMO", rawPayload.strip(), "valid-signature");

    var updated = paymentRepository.findByInternalReference(ref).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
    assertThat(updated.isVerified()).isTrue();

    var booking = bookingRepository.findById(pendingBooking.getId()).orElseThrow();
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
  }

  // ── helpers ──

  private void ensureRule() {
    if (ruleRepository.findLatest().isEmpty()) {
      CommissionRule rule = new CommissionRule();
      rule.setDriverCommissionPct(new BigDecimal("0.1000"));
      rule.setPassengerServiceFeePct(new BigDecimal("0.0300"));
      rule.setMinServiceFee(BigDecimal.ZERO);
      rule.setFixedBookingFee(BigDecimal.ZERO);
      rule.setCancellationFeePct(BigDecimal.ZERO);
      rule.setEffectiveFrom(Instant.now().minus(1, ChronoUnit.HOURS));
      ruleRepository.save(rule);
    }
  }

  private User user(String email) {
    User u = new User();
    u.setEmail(email); u.setPasswordHash("hash");
    u.setFirstName("Test"); u.setLastName("User");
    u.setPhone("+250788" + SEQ.incrementAndGet());
    u.setStatus(UserStatus.ACTIVE);
    u.setRoles(new HashSet<>(Set.of(Role.PASSENGER)));
    return userRepository.save(u);
  }

  private Location loc(String name) {
    Location l = new Location();
    l.setName(name); l.setRegion("Test"); l.setCountry("Rwanda"); l.setActive(true);
    return locationRepository.save(l);
  }
}
