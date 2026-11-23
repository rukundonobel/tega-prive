package com.tegaprive.vehicles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.DocumentStatus;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.storage.StorageService;
import com.tegaprive.users.User;
import com.tegaprive.vehicles.dto.VehicleDocumentDto;
import com.tegaprive.vehicles.dto.VehicleDto;
import com.tegaprive.vehicles.dto.VehiclePhotoDto;
import com.tegaprive.vehicles.dto.VehicleRequest;

/** Vehicle CRUD, photos, documents, submission for approval, and admin approval. */
@Service
public class VehicleService {

  private final VehicleRepository vehicleRepository;
  private final VehiclePhotoRepository photoRepository;
  private final VehicleDocumentRepository documentRepository;
  private final DriverRepository driverRepository;
  private final StorageService storageService;

  public VehicleService(VehicleRepository vehicleRepository, VehiclePhotoRepository photoRepository,
      VehicleDocumentRepository documentRepository, DriverRepository driverRepository,
      StorageService storageService) {
    this.vehicleRepository = vehicleRepository;
    this.photoRepository = photoRepository;
    this.documentRepository = documentRepository;
    this.driverRepository = driverRepository;
    this.storageService = storageService;
  }

  @Transactional
  public VehicleDto createVehicle(UUID userId, VehicleRequest request) {
    Driver driver = requireDriver(userId);
    if (vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
      throw new ConflictException("A vehicle with this registration number already exists");
    }
    Vehicle vehicle = new Vehicle();
    vehicle.setDriver(driver);
    applyRequest(vehicle, request);
    vehicle.setApprovalStatus(VehicleApprovalStatus.DRAFT);
    vehicleRepository.save(vehicle);
    return toDto(vehicle, driver);
  }

  @Transactional(readOnly = true)
  public List<VehicleDto> listMyVehicles(UUID userId) {
    Driver driver = requireDriver(userId);
    return vehicleRepository.findByDriverId(driver.getId()).stream()
        .map(vehicle -> toDto(vehicle, driver))
        .toList();
  }

  @Transactional(readOnly = true)
  public VehicleDto getMyVehicle(UUID userId, UUID vehicleId) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    return toDto(vehicle, driver);
  }

  @Transactional
  public VehicleDto updateVehicle(UUID userId, UUID vehicleId, VehicleRequest request) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    if (vehicle.getApprovalStatus() == VehicleApprovalStatus.APPROVED) {
      throw new BadRequestException("An approved vehicle cannot be edited");
    }
    if (!vehicle.getRegistrationNumber().equalsIgnoreCase(request.registrationNumber())
        && vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
      throw new ConflictException("A vehicle with this registration number already exists");
    }
    applyRequest(vehicle, request);
    vehicleRepository.save(vehicle);
    return toDto(vehicle, driver);
  }

  @Transactional
  public void deleteVehicle(UUID userId, UUID vehicleId) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    if (vehicle.getApprovalStatus() == VehicleApprovalStatus.APPROVED) {
      throw new BadRequestException("An approved vehicle cannot be deleted");
    }
    for (VehiclePhoto photo : photoRepository.findByVehicleIdOrderByPositionAsc(vehicle.getId())) {
      storageService.delete(photo.getPublicId());
    }
    for (VehicleDocument document : documentRepository.findByVehicleId(vehicle.getId())) {
      storageService.delete(document.getPublicId());
    }
    vehicleRepository.delete(vehicle);
  }

  @Transactional
  public VehiclePhotoDto addPhoto(UUID userId, UUID vehicleId, MultipartFile file) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    StorageService.UploadResult result = storageService.upload(file, "vehicles");
    VehiclePhoto photo = new VehiclePhoto();
    photo.setVehicle(vehicle);
    photo.setUrl(result.url());
    photo.setPublicId(result.publicId());
    int nextPosition = photoRepository.findTopByVehicleIdOrderByPositionDesc(vehicle.getId())
        .map(VehiclePhoto::getPosition).orElse(-1) + 1;
    photo.setPosition(nextPosition);
    photo.setPrimary(nextPosition == 0);
    photoRepository.save(photo);
    return VehicleMapper.toPhotoDto(photo);
  }

  @Transactional
  public void removePhoto(UUID userId, UUID vehicleId, UUID photoId) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    VehiclePhoto photo = photoRepository.findById(photoId)
        .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
    if (!photo.getVehicle().getId().equals(vehicle.getId())) {
      throw new ResourceNotFoundException("Photo not found");
    }
    storageService.delete(photo.getPublicId());
    boolean wasPrimary = photo.isPrimary();
    photoRepository.delete(photo);
    if (wasPrimary) {
      photoRepository.findByVehicleIdOrderByPositionAsc(vehicle.getId()).stream().findFirst()
          .ifPresent(first -> {
            first.setPrimary(true);
            photoRepository.save(first);
          });
    }
  }

  @Transactional
  public VehicleDocumentDto addDocument(UUID userId, UUID vehicleId, VehicleDocumentType type, MultipartFile file) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    StorageService.UploadResult result = storageService.upload(file, "vehicle-documents");
    VehicleDocument document = new VehicleDocument();
    document.setVehicle(vehicle);
    document.setType(type);
    document.setUrl(result.url());
    document.setPublicId(result.publicId());
    document.setStatus(DocumentStatus.PENDING);
    documentRepository.save(document);
    return VehicleMapper.toDocumentDto(document);
  }

  @Transactional
  public VehicleDto submitForApproval(UUID userId, UUID vehicleId) {
    Driver driver = requireDriver(userId);
    Vehicle vehicle = ownedVehicle(driver, vehicleId);
    if (vehicle.getApprovalStatus() == VehicleApprovalStatus.APPROVED) {
      throw new BadRequestException("Vehicle is already approved");
    }
    if (photoRepository.findByVehicleIdOrderByPositionAsc(vehicle.getId()).isEmpty()) {
      throw new BadRequestException("Add at least one photo before submitting");
    }
    if (!documentRepository.existsByVehicleIdAndType(vehicle.getId(), VehicleDocumentType.REGISTRATION)) {
      throw new BadRequestException("Add a registration document before submitting");
    }
    vehicle.setApprovalStatus(VehicleApprovalStatus.PENDING);
    vehicle.setRejectionReason(null);
    vehicleRepository.save(vehicle);
    return toDto(vehicle, driver);
  }

  // ---- Admin ----

  @Transactional(readOnly = true)
  public List<VehicleDto> listPendingVehicles() {
    return vehicleRepository.findByApprovalStatus(VehicleApprovalStatus.PENDING).stream()
        .map(vehicle -> toDto(vehicle, vehicle.getDriver()))
        .toList();
  }

  @Transactional(readOnly = true)
  public VehicleDto getVehicleForAdmin(UUID vehicleId) {
    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    return toDto(vehicle, vehicle.getDriver());
  }

  @Transactional
  public VehicleDto approveVehicle(UUID vehicleId, UUID adminId) {
    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    vehicle.setApprovalStatus(VehicleApprovalStatus.APPROVED);
    vehicle.setApprovedAt(Instant.now());
    vehicle.setApprovedBy(adminId);
    vehicle.setRejectionReason(null);
    vehicleRepository.save(vehicle);
    return toDto(vehicle, vehicle.getDriver());
  }

  @Transactional
  public VehicleDto rejectVehicle(UUID vehicleId, String reason, UUID adminId) {
    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    vehicle.setApprovalStatus(VehicleApprovalStatus.REJECTED);
    vehicle.setRejectionReason(reason);
    vehicle.setApprovedBy(adminId);
    vehicle.setApprovedAt(Instant.now());
    vehicleRepository.save(vehicle);
    return toDto(vehicle, vehicle.getDriver());
  }

  // ---- helpers ----

  private Driver requireDriver(UUID userId) {
    return driverRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
  }

  private Vehicle ownedVehicle(Driver driver, UUID vehicleId) {
    return vehicleRepository.findByIdAndDriverId(vehicleId, driver.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
  }

  private String driverName(Driver driver) {
    User user = driver.getUser();
    return (user.getFirstName() + " " + user.getLastName()).trim();
  }

  private VehicleDto toDto(Vehicle vehicle, Driver driver) {
    return VehicleMapper.toDto(vehicle, driver.getId(), driverName(driver),
        photoRepository.findByVehicleIdOrderByPositionAsc(vehicle.getId()),
        documentRepository.findByVehicleId(vehicle.getId()));
  }

  private void applyRequest(Vehicle vehicle, VehicleRequest request) {
    vehicle.setMake(request.make());
    vehicle.setModel(request.model());
    vehicle.setManufacturingYear(request.manufacturingYear());
    vehicle.setColour(request.colour());
    vehicle.setRegistrationNumber(request.registrationNumber());
    vehicle.setCategory(request.category());
    vehicle.setSeatCapacity(request.seatCapacity());
    vehicle.setLuggageCapacity(request.luggageCapacity());
    vehicle.setHasAc(request.hasAc());
    vehicle.setDescription(request.description());
  }
}
