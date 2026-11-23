package com.tegaprive.drivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.RegisterDriverRequest;
import com.tegaprive.storage.StorageService;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;
import com.tegaprive.users.UserStatus;

/** Driver registration, documents and admin verification (H2, mocked storage). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DriverServiceTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final AtomicInteger PHONE_SEQ = new AtomicInteger(1000);

  @Autowired
  private DriverService driverService;
  @Autowired
  private UserRepository userRepository;

  @MockitoBean
  private StorageService storageService;

  @BeforeEach
  void stubStorage() {
    when(storageService.upload(any(), anyString()))
        .thenReturn(new StorageService.UploadResult("http://test/driver-doc", "test-pid"));
  }

  @Test
  void registrationCreatesDriverAndAddsDriverRole() {
    User user = newUser("drv1@example.com");
    DriverDto driver = driverService.registerAsDriver(user.getId(),
        new RegisterDriverRequest("RW-LIC-1", null));

    assertThat(driver.verificationStatus()).isEqualTo("PENDING");
    assertThat(driver.verified()).isFalse();
    assertThat(driver.licenceNumber()).isEqualTo("RW-LIC-1");

    User reloaded = userRepository.findById(user.getId()).orElseThrow();
    assertThat(reloaded.getRoles()).contains(Role.DRIVER);
  }

  @Test
  void duplicateRegistrationFails() {
    User user = newUser("drv2@example.com");
    driverService.registerAsDriver(user.getId(), new RegisterDriverRequest("RW-LIC-2", null));
    assertThatThrownBy(() -> driverService.registerAsDriver(user.getId(),
        new RegisterDriverRequest("RW-LIC-2", null))).isInstanceOf(ConflictException.class);
  }

  @Test
  void addsDocumentForVerification() {
    User user = newUser("drv3@example.com");
    driverService.registerAsDriver(user.getId(), new RegisterDriverRequest("RW-LIC-3", null));

    var document = driverService.addDocument(user.getId(), DriverDocumentType.LICENCE,
        new MockMultipartFile("file", "licence.pdf", "application/pdf", new byte[] {1, 2, 3}));

    assertThat(document.type()).isEqualTo("LICENCE");
    assertThat(document.status()).isEqualTo("PENDING");
    assertThat(document.url()).isNotBlank();
  }

  @Test
  void adminCanApproveAndRejectDriver() {
    User user = newUser("drv4@example.com");
    DriverDto driver = driverService.registerAsDriver(user.getId(),
        new RegisterDriverRequest("RW-LIC-4", null));
    UUID driverId = UUID.fromString(driver.id());

    DriverDto approved = driverService.approveDriver(driverId, ADMIN_ID);
    assertThat(approved.verificationStatus()).isEqualTo("APPROVED");
    assertThat(approved.verified()).isTrue();

    DriverDto rejected = driverService.rejectDriver(driverId, "Blurry licence", ADMIN_ID);
    assertThat(rejected.verificationStatus()).isEqualTo("REJECTED");
    assertThat(rejected.rejectionReason()).isEqualTo("Blurry licence");
    assertThat(rejected.verified()).isFalse();
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
