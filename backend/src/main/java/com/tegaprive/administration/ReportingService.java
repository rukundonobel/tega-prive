package com.tegaprive.administration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.administration.dto.*;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.commissions.CommissionTransactionRepository;
import com.tegaprive.commissions.DriverEarningRepository;
import com.tegaprive.complaints.ComplaintRepository;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.journeys.JourneyRepository;
import com.tegaprive.payments.PaymentRepository;
import com.tegaprive.payments.PaymentStatus;
import com.tegaprive.reviews.ReviewRepository;
import com.tegaprive.tracking.GpsLocationRepository;
import com.tegaprive.users.UserRepository;
import com.tegaprive.vehicles.VehicleRepository;
import com.tegaprive.vehicles.VehicleApprovalStatus;
import com.tegaprive.journeys.JourneyStatus;

/** Aggregates platform-wide statistics for the admin dashboard and reports. */
@Service
@Transactional(readOnly = true)
public class ReportingService {

  private final BookingRepository bookingRepo;
  private final JourneyRepository journeyRepo;
  private final UserRepository userRepo;
  private final DriverRepository driverRepo;
  private final VehicleRepository vehicleRepo;
  private final PaymentRepository paymentRepo;
  private final CommissionTransactionRepository commissionTxRepo;
  private final DriverEarningRepository earningRepo;
  private final ComplaintRepository complaintRepo;
  private final ReviewRepository reviewRepo;
  private final GpsLocationRepository gpsRepo;

  public ReportingService(BookingRepository bookingRepo, JourneyRepository journeyRepo,
      UserRepository userRepo, DriverRepository driverRepo,
      VehicleRepository vehicleRepo, PaymentRepository paymentRepo,
      CommissionTransactionRepository commissionTxRepo,
      DriverEarningRepository earningRepo,
      ComplaintRepository complaintRepo, ReviewRepository reviewRepo,
      GpsLocationRepository gpsRepo) {
    this.bookingRepo = bookingRepo;
    this.journeyRepo = journeyRepo;
    this.userRepo = userRepo;
    this.driverRepo = driverRepo;
    this.vehicleRepo = vehicleRepo;
    this.paymentRepo = paymentRepo;
    this.commissionTxRepo = commissionTxRepo;
    this.earningRepo = earningRepo;
    this.complaintRepo = complaintRepo;
    this.reviewRepo = reviewRepo;
    this.gpsRepo = gpsRepo;
  }

  public DashboardStatsDto getDashboardStats() {
    long totalUsers = userRepo.count();
    long totalDrivers = driverRepo.count();
    long verifiedDrivers = driverRepo.findByVerificationStatus(
        com.tegaprive.drivers.VerificationStatus.APPROVED).size();
    long totalVehicles = vehicleRepo.count();
    long approvedVehicles = vehicleRepo.findByApprovalStatus(VehicleApprovalStatus.APPROVED).size();
    long totalJourneys = journeyRepo.count();
    long activeJourneys = journeyRepo.findAll().stream()
        .filter(j -> j.getStatus() == JourneyStatus.ACTIVE).count();
    long totalBookings = bookingRepo.count();
    long confirmedBookings = bookingRepo.findAll().stream()
        .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
            || b.getStatus() == BookingStatus.COMPLETED).count();

    BigDecimal totalRevenue = commissionTxRepo.findAll().stream()
        .map(tx -> tx.getPlatformRevenue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalDriverEarnings = commissionTxRepo.findAll().stream()
        .map(tx -> tx.getDriverNet())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    long pendingComplaints = complaintRepo.countByStatus("OPEN");
    long pendingPayouts = earningRepo.findAll().stream()
        .filter(e -> "PENDING".equals(e.getSettlementStatus())).count();

    return new DashboardStatsDto(
        totalUsers, totalDrivers, verifiedDrivers,
        totalVehicles, approvedVehicles,
        totalJourneys, activeJourneys,
        totalBookings, confirmedBookings,
        totalRevenue, totalDriverEarnings,
        pendingComplaints, pendingPayouts);
  }

  public List<BookingSummaryDto> recentBookings(int limit) {
    return bookingRepo.findAll().stream()
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .limit(limit)
        .map(b -> new BookingSummaryDto(
            b.getId().toString(),
            b.getJourney().getOrigin().getName(),
            b.getJourney().getDestination().getName(),
            b.getPassenger().getFirstName() + " " + b.getPassenger().getLastName(),
            b.getJourney().getDriver().getUser().getFirstName() + " " +
                b.getJourney().getDriver().getUser().getLastName(),
            b.getSeatsBooked(),
            b.getTotalAmount(),
            b.getStatus().name(),
            b.getCreatedAt()))
        .toList();
  }

  public List<ActiveJourneyDto> activeJourneys() {
    return journeyRepo.findAll().stream()
        .filter(j -> j.getStatus() == JourneyStatus.ACTIVE)
        .map(j -> {
          var driver = j.getDriver().getUser();
          var latestGps = gpsRepo.findLatestByJourneyId(j.getId());
          return new ActiveJourneyDto(
              j.getId().toString(),
              driver.getFirstName() + " " + driver.getLastName(),
              j.getVehicle().getMake() + " " + j.getVehicle().getModel(),
              j.getVehicle().getRegistrationNumber(),
              j.getOrigin().getName(),
              j.getDestination().getName(),
              j.getDepartureTime(),
              j.getTotalSeats() - j.getAvailableSeats(),
              j.getTotalSeats(),
              latestGps.map(g -> g.getLatitude().doubleValue()).orElse(null),
              latestGps.map(g -> g.getLongitude().doubleValue()).orElse(null),
              latestGps.map(g -> g.getRecordedAt()).orElse(null));
        })
        .toList();
  }

  public RevenueReportDto revenueReport(int days) {
    Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
    var txs = commissionTxRepo.findAll().stream()
        .filter(tx -> tx.getCreatedAt().isAfter(from))
        .toList();

    BigDecimal grossFare = txs.stream().map(t -> t.getGrossFare())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal platformRevenue = txs.stream().map(t -> t.getPlatformRevenue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal driverNet = txs.stream().map(t -> t.getDriverNet())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    long bookingCount = txs.size();

    return new RevenueReportDto(days, grossFare, platformRevenue, driverNet, bookingCount);
  }
}
