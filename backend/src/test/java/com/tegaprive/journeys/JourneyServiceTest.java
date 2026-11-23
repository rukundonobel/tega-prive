package com.tegaprive.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.drivers.VerificationStatus;
import com.tegaprive.journeys.dto.JourneyDto;
import com.tegaprive.journeys.dto.JourneyRequest;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;
import com.tegaprive.vehicles.Vehicle;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.vehicles.VehicleCategory;
import com.tegaprive.vehicles.VehicleRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JourneyServiceTest {

  private static final AtomicInteger SEQ = new AtomicInteger(500);

  @Autowired private JourneyService journeyService;
  @Autowired private UserRepository userRepository;
  @Autowired private DriverRepository driverRepository;
  @Autowired private VehicleRepository vehicleRepository;
  @Autowired private LocationRepository locationRepository;

  private UUID driverUserId;
  private UUID vehicleId;
  private UUID originId;
  private UUID destinationId;

  @BeforeEach
  void setup() {
    User driverUser = user("jdrv-" + SEQ.incrementAndGet() + "@test.rw");
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
    vehicle.setMake("Toyota");
    vehicle.setModel("RAV4");
    vehicle.setColour("Black");
    vehicle.setRegistrationNumber("RAC-" + SEQ.incrementAndGet());
    vehicle.setCategory(VehicleCategory.SUV);
    vehicle.setSeatCapacity(4);
    vehicle.setLuggageCapacity(2);
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicleRepository.save(vehicle);
    vehicleId = vehicle.getId();

    Location origin = loc("Kigali-J" + SEQ.incrementAndGet());
    Location dest = loc("Rubavu-J" + SEQ.incrementAndGet());
    originId = origin.getId();
    destinationId = dest.getId();
  }

  @Test
  void publishesJourneyForVerifiedDriverWithApprovedVehicle() {
    JourneyDto dto = journeyService.publish(driverUserId, request());
    assertThat(dto.status()).isEqualTo("SCHEDULED");
    assertThat(dto.availableSeats()).isEqualTo(4);
    assertThat(dto.totalSeats()).isEqualTo(4);
    assertThat(dto.pricePerSeat()).isEqualByComparingTo(new BigDecimal("7000"));
  }

  @Test
  void rejectsUnapprovedVehicle() {
    Vehicle pending = new Vehicle();
    Driver driver = driverRepository.findByUserId(driverUserId).orElseThrow();
    pending.setDriver(driver);
    pending.setMake("Toyota");
    pending.setModel("Prado");
    pending.setColour("Red");
    pending.setRegistrationNumber("RAD-" + SEQ.incrementAndGet());
    pending.setCategory(VehicleCategory.SUV);
    pending.setSeatCapacity(4);
    pending.setLuggageCapacity(2);
    pending.setApprovalStatus(VehicleApprovalStatus.PENDING);
    vehicleRepository.save(pending);

    JourneyRequest req = new JourneyRequest(pending.getId(), originId, destinationId,
        Instant.now().plus(2, ChronoUnit.HOURS), null,
        BigDecimal.valueOf(7000), 4, null, null, null);

    assertThatThrownBy(() -> journeyService.publish(driverUserId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("approved");
  }

  @Test
  void searchReturnsMatchingJourneys() {
    journeyService.publish(driverUserId, request());
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    List<JourneyDto> results = journeyService.search(originId, destinationId, tomorrow, 1);
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).originId()).isEqualTo(originId.toString());
    assertThat(results.get(0).destinationId()).isEqualTo(destinationId.toString());
  }

  @Test
  void startCompleteLifecycle() {
    JourneyDto published = journeyService.publish(driverUserId, request());
    UUID jid = UUID.fromString(published.id());

    JourneyDto started = journeyService.startJourney(driverUserId, jid);
    assertThat(started.status()).isEqualTo("ACTIVE");

    JourneyDto completed = journeyService.completeJourney(driverUserId, jid);
    assertThat(completed.status()).isEqualTo("COMPLETED");
  }

  private JourneyRequest request() {
    return new JourneyRequest(
        vehicleId, originId, destinationId,
        Instant.now().plus(26, ChronoUnit.HOURS), null,
        BigDecimal.valueOf(7000), 4, "Meet at city park", null, null);
  }

  private User user(String email) {
    User u = new User();
    u.setEmail(email);
    u.setPasswordHash("hash");
    u.setFirstName("Driver");
    u.setLastName("Test");
    u.setPhone("+250788" + SEQ.incrementAndGet());
    u.setStatus(UserStatus.ACTIVE);
    u.setRoles(new HashSet<>(Set.of(Role.DRIVER)));
    return userRepository.save(u);
  }

  private Location loc(String name) {
    Location l = new Location();
    l.setName(name);
    l.setRegion("Test");
    l.setCountry("Rwanda");
    l.setActive(true);
    return locationRepository.save(l);
  }
}
