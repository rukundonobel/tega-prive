package com.tegaprive.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.Booking;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.drivers.VerificationStatus;
import com.tegaprive.journeys.Journey;
import com.tegaprive.journeys.JourneyRepository;
import com.tegaprive.journeys.JourneyStatus;
import com.tegaprive.journeys.Location;
import com.tegaprive.journeys.LocationRepository;
import com.tegaprive.tracking.dto.LocationUpdate;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.vehicles.VehicleCategory;
import com.tegaprive.vehicles.VehicleRepository;

/**
 * Tests GPS ingestion: persistence, authorisation, broadcast, and freshness.
 * SimpMessagingTemplate is mocked — WebSocket broker is not started in unit tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrackingServiceTest {

  private static final AtomicInteger SEQ = new AtomicInteger(900);

  @Autowired TrackingService trackingService;
  @Autowired GpsLocationRepository gpsRepository;
  @Autowired JourneyRepository journeyRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired UserRepository userRepository;
  @Autowired VehicleRepository vehicleRepository;
  @Autowired LocationRepository locationRepository;
  @Autowired BookingRepository bookingRepository;

  @MockitoBean
  SimpMessagingTemplate messagingTemplate;

  private UUID driverUserId;
  private UUID passengerUserId;
  private UUID journeyId;

  @BeforeEach
  void setup() {
    User driverUser = user("trk-drv-" + SEQ.incrementAndGet() + "@test.rw");
    driverUserId = driverUser.getId();

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
    vehicle.setColour("Black");
    vehicle.setRegistrationNumber("TRK-" + SEQ.incrementAndGet());
    vehicle.setCategory(VehicleCategory.SEDAN);
    vehicle.setSeatCapacity(4); vehicle.setLuggageCapacity(2);
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicleRepository.save(vehicle);

    Location origin = loc("TRK-KGL-" + SEQ.incrementAndGet());
    Location dest   = loc("TRK-MSZ-" + SEQ.incrementAndGet());

    Journey journey = new Journey();
    journey.setDriver(driver); journey.setVehicle(vehicle);
    journey.setOrigin(origin); journey.setDestination(dest);
    journey.setDepartureTime(Instant.now().minus(30, ChronoUnit.MINUTES));
    journey.setPricePerSeat(BigDecimal.valueOf(5000));
    journey.setTotalSeats(4); journey.setAvailableSeats(2);
    journey.setStatus(JourneyStatus.ACTIVE);  // must be ACTIVE to accept GPS
    journeyRepository.save(journey);
    journeyId = journey.getId();

    // A passenger with a confirmed booking
    User passengerUser = user("trk-pas-" + SEQ.incrementAndGet() + "@test.rw");
    passengerUserId = passengerUser.getId();

    Booking booking = new Booking();
    booking.setJourney(journey);
    booking.setPassenger(passengerUser);
    booking.setSeatsBooked(1);
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setGrossFare(BigDecimal.valueOf(5000));
    booking.setServiceFee(BigDecimal.valueOf(150));
    booking.setTotalAmount(BigDecimal.valueOf(5150));
    booking.setConfirmedAt(Instant.now());
    bookingRepository.save(booking);
  }

  @Test
  void ingestionPersistsLocationAndBroadcasts() {
    var update = new LocationUpdate(
        journeyId.toString(),
        new BigDecimal("-1.9403"),
        new BigDecimal("30.0599"),
        new BigDecimal("270"),
        new BigDecimal("60"),
        new BigDecimal("5"),
        "BROWSER",
        System.currentTimeMillis());

    trackingService.ingestLocation(driverUserId, update);

    // Verify persisted
    var saved = gpsRepository.findLatestByJourneyId(journeyId);
    assertThat(saved).isPresent();
    assertThat(saved.get().getLatitude()).isEqualByComparingTo(new BigDecimal("-1.9403"));

    // Verify broadcast
    verify(messagingTemplate, times(1))
        .convertAndSend(anyString(), any(LocationUpdate.class));
  }

  @Test
  void rejectsNonDriverSendingLocation() {
    var update = new LocationUpdate(
        journeyId.toString(),
        new BigDecimal("-1.9403"),
        new BigDecimal("30.0599"),
        null, null, null, "BROWSER", System.currentTimeMillis());

    assertThatThrownBy(() -> trackingService.ingestLocation(passengerUserId, update))
        .isInstanceOf(Exception.class); // passenger has no driver profile → ResourceNotFound
  }

  @Test
  void rejectsGpsOnInactiveJourney() {
    // Switch journey to SCHEDULED (not ACTIVE)
    Journey j = journeyRepository.findById(journeyId).orElseThrow();
    j.setStatus(JourneyStatus.SCHEDULED);
    journeyRepository.save(j);

    var update = new LocationUpdate(
        journeyId.toString(),
        new BigDecimal("-1.9403"),
        new BigDecimal("30.0599"),
        null, null, null, "BROWSER", System.currentTimeMillis());

    assertThatThrownBy(() -> trackingService.ingestLocation(driverUserId, update))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not active");
  }

  @Test
  void passengerCanGetStateWithConfirmedBooking() {
    // Send a fix first
    var update = new LocationUpdate(
        journeyId.toString(),
        new BigDecimal("-1.9500"),
        new BigDecimal("30.1000"),
        null, null, null, "BROWSER", System.currentTimeMillis());
    trackingService.ingestLocation(driverUserId, update);

    var state = trackingService.getState(passengerUserId, journeyId);
    assertThat(state.latitude()).isEqualByComparingTo(new BigDecimal("-1.9500"));
    assertThat(state.fresh()).isTrue();
  }

  @Test
  void unauthorisedUserCannotGetState() {
    User stranger = user("trk-str-" + SEQ.incrementAndGet() + "@test.rw");
    assertThatThrownBy(() -> trackingService.getState(stranger.getId(), journeyId))
        .isInstanceOf(BadRequestException.class);
  }

  // ── helpers ──

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
