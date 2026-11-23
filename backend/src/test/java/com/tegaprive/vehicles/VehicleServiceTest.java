package com.tegaprive.vehicles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.DriverService;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.RegisterDriverRequest;
import com.tegaprive.storage.StorageService;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;
import com.tegaprive.vehicles.dto.VehicleDto;
import com.tegaprive.vehicles.dto.VehicleRequest;

/** Full vehicle lifecycle: create, submit, approve/reject, edit/delete rules, ownership. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehicleServiceTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final AtomicInteger PHONE_SEQ = new AtomicInteger(2000);

  @Autowired
  private VehicleService vehicleService;
  @Autowired
  private DriverService driverService;
  @Autowired
  private UserRepository userRepository;

  @MockitoBean
  private StorageService storageService;

  private UUID driverUserId;
  private UUID driverId;

  @BeforeEach
  void setUp() {
    when(storageService.upload(any(), anyString()))
        .thenReturn(new StorageService.UploadResult("http://test/file", "test-pid"));
    User user = newUser("vehicle-driver@example.com");
    driverUserId = user.getId();
    DriverDto createdDriver = driverService.registerAsDriver(driverUserId,
        new RegisterDriverRequest("RW-LIC-V", LocalDate.of(2030, 1, 1)));
    driverId = UUID.fromString(createdDriver.id());
  }

  @Test
  void createsVehicleAsDraft() {
    VehicleDto vehicle = vehicleService.createVehicle(driverUserId, request("RAB-100"));
    assertThat(vehicle.approvalStatus()).isEqualTo("DRAFT");
    assertThat(vehicle.driverId()).isEqualTo(driverId.toString());
    assertThat(vehicle.photos()).isEmpty();
  }

  @Test
  void duplicateRegistrationNumberFails() {
    vehicleService.createVehicle(driverUserId, request("RAB-DUP"));
    assertThatThrownBy(() -> vehicleService.createVehicle(driverUserId, request("RAB-DUP")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void submitRequiresPhotoAndRegistrationDocument() {
    VehicleDto vehicle = vehicleService.createVehicle(driverUserId, request("RAB-101"));
    UUID id = UUID.fromString(vehicle.id());

    assertThatThrownBy(() -> vehicleService.submitForApproval(driverUserId, id))
        .isInstanceOf(BadRequestException.class);

    vehicleService.addPhoto(driverUserId, id, image());
    // Still missing the registration document.
    assertThatThrownBy(() -> vehicleService.submitForApproval(driverUserId, id))
        .isInstanceOf(BadRequestException.class);

    vehicleService.addDocument(driverUserId, id, VehicleDocumentType.REGISTRATION, pdf());
    VehicleDto submitted = vehicleService.submitForApproval(driverUserId, id);
    assertThat(submitted.approvalStatus()).isEqualTo("PENDING");
  }

  @Test
  void adminApprovesVehicle() {
    UUID id = prepareSubmittableVehicle("RAB-200");
    VehicleDto approved = vehicleService.approveVehicle(id, ADMIN_ID);
    assertThat(approved.approvalStatus()).isEqualTo("APPROVED");
  }

  @Test
  void adminRejectsVehicleWithReason() {
    UUID id = prepareSubmittableVehicle("RAB-201");
    VehicleDto rejected = vehicleService.rejectVehicle(id, "Insurance expired", ADMIN_ID);
    assertThat(rejected.approvalStatus()).isEqualTo("REJECTED");
    assertThat(rejected.rejectionReason()).isEqualTo("Insurance expired");
  }

  @Test
  void approvedVehicleCannotBeEditedOrDeleted() {
    UUID id = prepareSubmittableVehicle("RAB-202");
    vehicleService.approveVehicle(id, ADMIN_ID);

    assertThatThrownBy(() -> vehicleService.updateVehicle(driverUserId, id, request("RAB-202")))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> vehicleService.deleteVehicle(driverUserId, id))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void anotherDriverCannotAccessVehicle() {
    UUID id = prepareSubmittableVehicle("RAB-203");

    User other = newUser("other-driver@example.com");
    driverService.registerAsDriver(other.getId(), new RegisterDriverRequest("RW-LIC-O", null));

    assertThatThrownBy(() -> vehicleService.getMyVehicle(other.getId(), id))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> vehicleService.updateVehicle(other.getId(), id, request("RAB-203")))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletesDraftVehicle() {
    VehicleDto vehicle = vehicleService.createVehicle(driverUserId, request("RAB-204"));
    UUID id = UUID.fromString(vehicle.id());
    vehicleService.deleteVehicle(driverUserId, id);
    assertThatThrownBy(() -> vehicleService.getMyVehicle(driverUserId, id))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private UUID prepareSubmittableVehicle(String registration) {
    VehicleDto vehicle = vehicleService.createVehicle(driverUserId, request(registration));
    UUID id = UUID.fromString(vehicle.id());
    vehicleService.addPhoto(driverUserId, id, image());
    vehicleService.addDocument(driverUserId, id, VehicleDocumentType.REGISTRATION, pdf());
    vehicleService.submitForApproval(driverUserId, id);
    return id;
  }

  private VehicleRequest request(String registration) {
    return new VehicleRequest("Toyota", "RAV4", 2022, "White", registration,
        VehicleCategory.SUV, 4, 3, true, "Comfortable SUV");
  }

  private MockMultipartFile image() {
    return new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3, 4});
  }

  private MockMultipartFile pdf() {
    return new MockMultipartFile("file", "reg.pdf", "application/pdf", new byte[] {1, 2, 3});
  }

  private User newUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setFirstName("Test");
    user.setLastName("Driver");
    user.setPhone("+250788" + PHONE_SEQ.incrementAndGet());
    user.setStatus(UserStatus.ACTIVE);
    user.setRoles(new HashSet<>(Set.of(Role.PASSENGER)));
    return userRepository.save(user);
  }
}
