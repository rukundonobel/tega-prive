package com.tegaprive.drivers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.dto.DriverDocumentDto;
import com.tegaprive.drivers.dto.DriverDto;
import com.tegaprive.drivers.dto.DriverSummaryDto;
import com.tegaprive.drivers.dto.RegisterDriverRequest;
import com.tegaprive.drivers.dto.UpdateDriverRequest;
import com.tegaprive.storage.StorageService;
import com.tegaprive.users.Role;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;

/** Driver registration, profile, documents, and admin verification. */
@Service
public class DriverService {

  private final DriverRepository driverRepository;
  private final DriverDocumentRepository documentRepository;
  private final UserRepository userRepository;
  private final StorageService storageService;

  public DriverService(DriverRepository driverRepository, DriverDocumentRepository documentRepository,
      UserRepository userRepository, StorageService storageService) {
    this.driverRepository = driverRepository;
    this.documentRepository = documentRepository;
    this.userRepository = userRepository;
    this.storageService = storageService;
  }

  @Transactional
  public DriverDto registerAsDriver(UUID userId, RegisterDriverRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (driverRepository.existsByUserId(userId)) {
      throw new ConflictException("A driver profile already exists for this account");
    }
    Driver driver = new Driver();
    driver.setUser(user);
    driver.setLicenceNumber(request.licenceNumber());
    driver.setLicenceExpiry(request.licenceExpiry());
    driver.setVerificationStatus(VerificationStatus.PENDING);
    driver.setVerified(false);
    driverRepository.save(driver);

    if (user.getRoles() == null) {
      user.setRoles(new HashSet<>());
    }
    user.getRoles().add(Role.DRIVER);
    userRepository.save(user);

    return DriverMapper.toDto(driver, user);
  }

  @Transactional(readOnly = true)
  public DriverDto getMyDriver(UUID userId) {
    Driver driver = requireDriver(userId);
    return DriverMapper.toDto(driver, driver.getUser());
  }

  @Transactional
  public DriverDto updateMyDriver(UUID userId, UpdateDriverRequest request) {
    Driver driver = requireDriver(userId);
    if (driver.getVerificationStatus() == VerificationStatus.APPROVED) {
      throw new BadRequestException("A verified driver profile cannot be edited; contact support");
    }
    driver.setLicenceNumber(request.licenceNumber());
    driver.setLicenceExpiry(request.licenceExpiry());
    driverRepository.save(driver);
    return DriverMapper.toDto(driver, driver.getUser());
  }

  @Transactional
  public DriverDto uploadProfilePhoto(UUID userId, MultipartFile file) {
    Driver driver = requireDriver(userId);
    StorageService.UploadResult result = storageService.upload(file, "drivers");
    driver.setProfilePhotoUrl(result.url());
    driverRepository.save(driver);
    return DriverMapper.toDto(driver, driver.getUser());
  }

  @Transactional
  public DriverDocumentDto addDocument(UUID userId, DriverDocumentType type, MultipartFile file) {
    Driver driver = requireDriver(userId);
    StorageService.UploadResult result = storageService.upload(file, "driver-documents");
    DriverDocument document = new DriverDocument();
    document.setDriver(driver);
    document.setType(type);
    document.setUrl(result.url());
    document.setPublicId(result.publicId());
    document.setStatus(DocumentStatus.PENDING);
    documentRepository.save(document);
    return DriverMapper.toDocumentDto(document);
  }

  @Transactional(readOnly = true)
  public List<DriverDocumentDto> listMyDocuments(UUID userId) {
    Driver driver = requireDriver(userId);
    return documentRepository.findByDriverId(driver.getId()).stream()
        .map(DriverMapper::toDocumentDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DriverSummaryDto> listAllDrivers() {
    return driverRepository.findAll().stream()
        .map(driver -> DriverMapper.toSummaryDto(driver, driver.getUser()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DriverSummaryDto> listPendingDrivers() {
    return driverRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
        .map(driver -> DriverMapper.toSummaryDto(driver, driver.getUser()))
        .toList();
  }

  @Transactional
  public DriverDto approveDriver(UUID driverId, UUID adminId) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    driver.setVerificationStatus(VerificationStatus.APPROVED);
    driver.setVerified(true);
    driver.setVerifiedAt(Instant.now());
    driver.setVerifiedBy(adminId);
    driver.setRejectionReason(null);
    driverRepository.save(driver);
    return DriverMapper.toDto(driver, driver.getUser());
  }

  @Transactional
  public DriverDto rejectDriver(UUID driverId, String reason, UUID adminId) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    driver.setVerificationStatus(VerificationStatus.REJECTED);
    driver.setVerified(false);
    driver.setVerifiedBy(adminId);
    driver.setVerifiedAt(Instant.now());
    driver.setRejectionReason(reason);
    driverRepository.save(driver);
    return DriverMapper.toDto(driver, driver.getUser());
  }

  private Driver requireDriver(UUID userId) {
    return driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
  }
}
