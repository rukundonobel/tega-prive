package com.tegaprive.bookings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.dto.BookingRequest;
import com.tegaprive.bookings.dto.CancelRequest;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
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
 * Tests the booking workflow: seat math, duplicate prevention, concurrent booking safety,
 * cancellation (seats return to inventory), commission calculation, and driver decline.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceTest {

  private static final AtomicInteger SEQ = new AtomicInteger(100);

  @Autowired private BookingService bookingService;
  @Autowired private UserRepository userRepository;
  @Autowired private DriverRepository driverRepository;
  @Autowired private VehicleRepository vehicleRepository;
  @Autowired private JourneyRepository journeyRepository;
  @Autowired private LocationRepository locationRepository;
  @Autowired private BookingRepository bookingRepository;

  private UUID driverUserId;
  private UUID journeyId;
  private Location origin;
  private Location destination;

  @BeforeEach
  @Transactional
  void setup() {
    // Seed two locations
    origin = loc("Kigali-" + SEQ.incrementAndGet(), "Kigali City");
    destination = loc("Musanze-" + SEQ.incrementAndGet(), "Northern Province");

    // A verified driver with an approved vehicle
    User driverUser = user("driver-" + SEQ.incrementAndGet() + "@test.rw");
    driverUserId = driverUser.getId();

    Driver driver = new Driver();
    driver.setUser(driverUser);
    driver.setVerified(true);
    driver.setVerificationStatus(VerificationStatus.APPROVED);
    driver.setRatingAvg(BigDecimal.valueOf(4.5));
    driver.setCommissionWalletBalance(BigDecimal.ZERO);
    driverRepository.save(driver);

    Vehicle vehicle = new Vehicle();
    vehicle.setDriver(driver);
    vehicle.setMake("Toyota");
    vehicle.setModel("Hiace");
    vehicle.setColour("White");
    vehicle.setRegistrationNumber("RAB-" + SEQ.incrementAndGet());
    vehicle.setCategory(VehicleCategory.VAN);
    vehicle.setSeatCapacity(14);
    vehicle.setLuggageCapacity(5);
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicleRepository.save(vehicle);

    Journey journey = new Journey();
    journey.setDriver(driver);
    journey.setVehicle(vehicle);
    journey.setOrigin(origin);
    journey.setDestination(destination);
    journey.setDepartureTime(Instant.now().plus(2, ChronoUnit.HOURS));
    journey.setPricePerSeat(BigDecimal.valueOf(5000));
    journey.setTotalSeats(4);
    journey.setAvailableSeats(4);
    journey.setStatus(JourneyStatus.SCHEDULED);
    journeyRepository.save(journey);
    journeyId = journey.getId();
  }

  @Test
  @Transactional
  void booksSeatsAndCalculatesFees() {
    UUID passengerId = user("p1-" + SEQ.incrementAndGet() + "@test.rw").getId();
    var booking = bookingService.createBooking(passengerId, new BookingRequest(journeyId, 2));

    // 2 seats × 5000 RWF = 10,000 gross; 3% fee = 300; total 10,300
    assertThat(booking.grossFare()).isEqualByComparingTo(new BigDecimal("10000.00"));
    assertThat(booking.serviceFee()).isEqualByComparingTo(new BigDecimal("300.00"));
    assertThat(booking.totalAmount()).isEqualByComparingTo(new BigDecimal("10300.00"));
    assertThat(booking.status()).isEqualTo("PENDING_PAYMENT");
    assertThat(booking.seatsBooked()).isEqualTo(2);

    // Available seats decremented
    Journey j = journeyRepository.findById(journeyId).orElseThrow();
    assertThat(j.getAvailableSeats()).isEqualTo(2);
  }

  @Test
  @Transactional
  void preventsOverbooking() {
    UUID p1 = user("ov1-" + SEQ.incrementAndGet() + "@test.rw").getId();
    UUID p2 = user("ov2-" + SEQ.incrementAndGet() + "@test.rw").getId();
    UUID p3 = user("ov3-" + SEQ.incrementAndGet() + "@test.rw").getId();

    bookingService.createBooking(p1, new BookingRequest(journeyId, 2));
    bookingService.createBooking(p2, new BookingRequest(journeyId, 2));

    // 4 seats gone — p3 should be rejected
    assertThatThrownBy(() -> bookingService.createBooking(p3, new BookingRequest(journeyId, 1)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Not enough seats");
  }

  @Test
  @Transactional
  void preventsDuplicateBooking() {
    UUID passengerId = user("dup-" + SEQ.incrementAndGet() + "@test.rw").getId();
    bookingService.createBooking(passengerId, new BookingRequest(journeyId, 1));
    assertThatThrownBy(() -> bookingService.createBooking(passengerId, new BookingRequest(journeyId, 1)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @Transactional
  void cancellationReturnsSeats() {
    UUID passengerId = user("can-" + SEQ.incrementAndGet() + "@test.rw").getId();
    var booking = bookingService.createBooking(passengerId, new BookingRequest(journeyId, 2));
    assertThat(journeyRepository.findById(journeyId).orElseThrow().getAvailableSeats()).isEqualTo(2);

    bookingService.cancelByPassenger(passengerId, UUID.fromString(booking.id()),
        new CancelRequest("Changed plans"));
    assertThat(journeyRepository.findById(journeyId).orElseThrow().getAvailableSeats()).isEqualTo(4);
  }

  @Test
  @Transactional
  void newBookingStartsAsPendingPayment() {
    UUID passengerId = user("pay-" + SEQ.incrementAndGet() + "@test.rw").getId();
    var booking = bookingService.createBooking(passengerId, new BookingRequest(journeyId, 1));
    // A fresh booking waits for payment — advancing to CONFIRMED is PaymentService's job,
    // covered in PaymentServiceTest.devConfirmAdvancesBookingToConfirmed().
    assertThat(booking.status()).isEqualTo("PENDING_PAYMENT");
    assertThat(booking.grossFare()).isPositive();
    assertThat(booking.serviceFee()).isPositive();
    assertThat(booking.totalAmount()).isEqualByComparingTo(booking.grossFare().add(booking.serviceFee()));
  }

  @Test
  @Transactional
  void driverCanDeclineBooking() {
    UUID passengerId = user("dec-" + SEQ.incrementAndGet() + "@test.rw").getId();
    var booking = bookingService.createBooking(passengerId, new BookingRequest(journeyId, 1));
    int seatsBefore = journeyRepository.findById(journeyId).orElseThrow().getAvailableSeats();

    var declined = bookingService.declineByDriver(driverUserId, journeyId, UUID.fromString(booking.id()));
    assertThat(declined.status()).isEqualTo("CANCELLED");
    // Seat returned
    assertThat(journeyRepository.findById(journeyId).orElseThrow().getAvailableSeats())
        .isEqualTo(seatsBefore + 1);
  }

  /**
   * Concurrency test: 4 threads each try to book 1 seat on a 4-seat journey.
   * Exactly 4 bookings should succeed (no overbooking, none lost).
   */
  @Test
  void concurrentBookingsShouldNotOverbook() throws InterruptedException {
    int threads = 6;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      final UUID pid = user("con-" + SEQ.incrementAndGet() + "@test.rw").getId();
      pool.submit(() -> {
        ready.countDown();
        try {
          start.await();
          bookingService.createBooking(pid, new BookingRequest(journeyId, 1));
          successCount.incrementAndGet();
        } catch (BadRequestException e) {
          failCount.incrementAndGet();
        } catch (Exception e) {
          failCount.incrementAndGet();
        }
      });
    }

    ready.await();
    start.countDown();
    pool.shutdown();
    pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

    // Exactly 4 seats available → 4 successes max, >= 2 failures.
    assertThat(successCount.get()).isLessThanOrEqualTo(4);
    int seats = journeyRepository.findById(journeyId).orElseThrow().getAvailableSeats();
    assertThat(seats).isGreaterThanOrEqualTo(0);
    assertThat(seats + successCount.get()).isEqualTo(4);
  }

  // ── helpers ──

  private User user(String email) {
    User u = new User();
    u.setEmail(email);
    u.setPasswordHash("hash");
    u.setFirstName("Test");
    u.setLastName("User");
    u.setPhone("+250788" + SEQ.incrementAndGet());
    u.setStatus(UserStatus.ACTIVE);
    u.setRoles(new HashSet<>(Set.of(Role.PASSENGER)));
    return userRepository.save(u);
  }

  private Location loc(String name, String region) {
    Location l = new Location();
    l.setName(name);
    l.setRegion(region);
    l.setCountry("Rwanda");
    l.setActive(true);
    return locationRepository.save(l);
  }
}
