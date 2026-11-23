package com.tegaprive.payments;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
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
import com.tegaprive.commissions.CommissionEngine;
import com.tegaprive.commissions.CommissionRule;
import com.tegaprive.commissions.CommissionRuleRepository;
import com.tegaprive.commissions.CommissionTransactionRepository;
import com.tegaprive.commissions.DriverEarningRepository;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.drivers.VerificationStatus;
import com.tegaprive.journeys.Journey;
import com.tegaprive.journeys.JourneyRepository;
import com.tegaprive.journeys.JourneyStatus;
import com.tegaprive.journeys.Location;
import com.tegaprive.journeys.LocationRepository;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.vehicles.VehicleCategory;
import com.tegaprive.vehicles.VehicleRepository;

/**
 * Tests the commission engine: correct split calculations, idempotency, and driver earnings recording.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommissionEngineTest {

  private static final AtomicInteger SEQ = new AtomicInteger(700);

  @Autowired CommissionEngine commissionEngine;
  @Autowired CommissionRuleRepository ruleRepository;
  @Autowired CommissionTransactionRepository txRepository;
  @Autowired DriverEarningRepository earningRepository;
  @Autowired BookingRepository bookingRepository;
  @Autowired UserRepository userRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired VehicleRepository vehicleRepository;
  @Autowired JourneyRepository journeyRepository;
  @Autowired LocationRepository locationRepository;
  @Autowired PaymentRepository paymentRepository;

  private Booking testBooking;
  private Payment testPayment;

  @BeforeEach
  void setup() {
    // Ensure a default rule exists (V4 migration seeds one, but H2 starts fresh)
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

    // Build the minimum required object graph
    User driverUser = user("cmt-drv-" + SEQ.incrementAndGet() + "@test.rw");
    Driver driver = new Driver();
    driver.setUser(driverUser);
    driver.setVerified(true);
    driver.setVerificationStatus(VerificationStatus.APPROVED);
    driver.setRatingAvg(BigDecimal.ZERO);
    driver.setCommissionWalletBalance(BigDecimal.ZERO);
    driverRepository.save(driver);

    Vehicle vehicle = new Vehicle();
    vehicle.setDriver(driver);
    vehicle.setMake("Toyota"); vehicle.setModel("Hiace");
    vehicle.setColour("White");
    vehicle.setRegistrationNumber("CMT-" + SEQ.incrementAndGet());
    vehicle.setCategory(VehicleCategory.VAN);
    vehicle.setSeatCapacity(14); vehicle.setLuggageCapacity(5);
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicleRepository.save(vehicle);

    Location origin = loc("CMT-KGL-" + SEQ.incrementAndGet());
    Location dest   = loc("CMT-MSZ-" + SEQ.incrementAndGet());

    Journey journey = new Journey();
    journey.setDriver(driver); journey.setVehicle(vehicle);
    journey.setOrigin(origin); journey.setDestination(dest);
    journey.setDepartureTime(Instant.now().plus(2, ChronoUnit.HOURS));
    journey.setPricePerSeat(new BigDecimal("8000"));
    journey.setTotalSeats(4); journey.setAvailableSeats(2);
    journey.setStatus(JourneyStatus.SCHEDULED);
    journeyRepository.save(journey);

    User passengerUser = user("cmt-pas-" + SEQ.incrementAndGet() + "@test.rw");

    Booking booking = new Booking();
    booking.setJourney(journey);
    booking.setPassenger(passengerUser);
    booking.setSeatsBooked(1);
    booking.setStatus(BookingStatus.CONFIRMED);
    // grossFare = 8000 × 1 = 8000; serviceFee = 240; total = 8240
    booking.setGrossFare(new BigDecimal("8000.00"));
    booking.setServiceFee(new BigDecimal("240.00"));
    booking.setTotalAmount(new BigDecimal("8240.00"));
    bookingRepository.save(booking);
    testBooking = booking;

    Payment payment = new Payment();
    payment.setBooking(booking);
    payment.setProvider(PaymentProvider.MTN_MOMO);
    payment.setInternalReference("TP-TEST-" + SEQ.incrementAndGet());
    payment.setAmount(new BigDecimal("8240.00"));
    payment.setCurrency("RWF");
    payment.setPhoneNumber("+250788000999");
    payment.setStatus(PaymentStatus.SUCCESSFUL);
    payment.setVerified(true);
    paymentRepository.save(payment);
    testPayment = payment;
  }

  @Test
  void calculatesCorrectCommissionSplit() {
    var result = commissionEngine.settle(testBooking, testPayment);

    // 10% driver commission on 8000 = 800
    assertThat(result.driverCommission()).isEqualByComparingTo(new BigDecimal("800.00"));
    // driver net = 8000 - 800 = 7200
    assertThat(result.driverNet()).isEqualByComparingTo(new BigDecimal("7200.00"));
    // passenger service fee (from booking) = 240
    assertThat(result.passengerServiceFee()).isEqualByComparingTo(new BigDecimal("240.00"));
    // platform revenue = 800 + 240 = 1040
    assertThat(result.platformRevenue()).isEqualByComparingTo(new BigDecimal("1040.00"));
  }

  @Test
  void recordsDriverEarning() {
    commissionEngine.settle(testBooking, testPayment);
    var earnings = earningRepository.findByDriverIdOrderByCreatedAtDesc(
        testBooking.getJourney().getDriver().getId());
    assertThat(earnings).hasSize(1);
    assertThat(earnings.get(0).getNetEarnings()).isEqualByComparingTo(new BigDecimal("7200.00"));
    assertThat(earnings.get(0).getSettlementStatus()).isEqualTo("PENDING");
  }

  @Test
  void settleIsIdempotent() {
    commissionEngine.settle(testBooking, testPayment);
    commissionEngine.settle(testBooking, testPayment); // second call must not create duplicate

    long txCount = txRepository.findAll().stream()
        .filter(tx -> tx.getBooking().getId().equals(testBooking.getId()))
        .count();
    long earningCount = earningRepository.findAll().stream()
        .filter(e -> e.getBooking().getId().equals(testBooking.getId()))
        .count();

    assertThat(txCount).isEqualTo(1);
    assertThat(earningCount).isEqualTo(1);
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
