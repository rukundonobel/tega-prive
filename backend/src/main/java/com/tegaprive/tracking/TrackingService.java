package com.tegaprive.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.Driver;
import com.tegaprive.drivers.DriverRepository;
import com.tegaprive.journeys.Journey;
import com.tegaprive.journeys.JourneyRepository;
import com.tegaprive.journeys.JourneyStatus;
import com.tegaprive.tracking.dto.LocationUpdate;
import com.tegaprive.tracking.dto.TrackingState;

/**
 * Handles:
 *   1. Persisting incoming GPS fixes from the driver.
 *   2. Broadcasting the fix to the journey's STOMP topic.
 *   3. Serving the latest GPS state for initial page-load (REST GET).
 *   4. Authorisation: only the driver who owns the journey can send fixes;
 *      only passengers with CONFIRMED bookings can poll/subscribe.
 */
@Service
public class TrackingService {

  private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
  /** Consider the GPS stale after this many seconds without an update. */
  private static final long FRESHNESS_SECONDS = 60;

  private final GpsLocationRepository gpsRepository;
  private final JourneyRepository journeyRepository;
  private final DriverRepository driverRepository;
  private final BookingRepository bookingRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public TrackingService(GpsLocationRepository gpsRepository,
      JourneyRepository journeyRepository,
      DriverRepository driverRepository,
      BookingRepository bookingRepository,
      SimpMessagingTemplate messagingTemplate) {
    this.gpsRepository = gpsRepository;
    this.journeyRepository = journeyRepository;
    this.driverRepository = driverRepository;
    this.bookingRepository = bookingRepository;
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Receives a GPS fix from the driver, persists it, and broadcasts it.
   * Called from the STOMP @MessageMapping handler.
   */
  @Transactional
  public void ingestLocation(UUID driverUserId, LocationUpdate update) {
    Driver driver = driverRepository.findByUserId(driverUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

    Journey journey = journeyRepository.findById(UUID.fromString(update.journeyId()))
        .orElseThrow(() -> new ResourceNotFoundException("Journey not found"));

    // Authorisation: only the owner driver can send updates.
    if (!journey.getDriver().getId().equals(driver.getId())) {
      throw new BadRequestException("Not the driver for this journey");
    }
    if (journey.getStatus() != JourneyStatus.ACTIVE) {
      throw new BadRequestException("Journey is not active — start it first");
    }

    // Persist the fix.
    GpsLocation fix = new GpsLocation();
    fix.setJourney(journey);
    fix.setDriver(driver);
    fix.setLatitude(update.latitude());
    fix.setLongitude(update.longitude());
    fix.setHeading(update.heading());
    fix.setSpeed(update.speed());
    fix.setAccuracy(update.accuracy());
    fix.setDevice(update.device() != null ? update.device() : "BROWSER");
    fix.setRecordedAt(Instant.ofEpochMilli(update.recordedAt() > 0
        ? update.recordedAt() : System.currentTimeMillis()));
    gpsRepository.save(fix);

    // Build the broadcast payload (identical shape to LocationUpdate, adds server timestamp).
    LocationUpdate broadcast = new LocationUpdate(
        update.journeyId(),
        update.latitude(),
        update.longitude(),
        update.heading(),
        update.speed(),
        update.accuracy(),
        fix.getDevice(),
        fix.getRecordedAt().toEpochMilli());

    // Broadcast only to subscribers of this journey's topic.
    // The StompAuthChannelInterceptor has already verified their booking.
    String destination = "/topic/journey/" + journey.getId() + "/location";
    messagingTemplate.convertAndSend(destination, broadcast);
    log.debug("Broadcast GPS for journey={} lat={} lon={}", journey.getId(),
        update.latitude(), update.longitude());
  }

  /**
   * Returns the latest known GPS state for a journey — called on REST GET at
   * page load so the passenger sees the last known position immediately.
   * Authorisation: passenger must have a CONFIRMED booking, or be the driver.
   */
  @Transactional(readOnly = true)
  public TrackingState getState(UUID userId, UUID journeyId) {
    Journey journey = journeyRepository.findById(journeyId)
        .orElseThrow(() -> new ResourceNotFoundException("Journey not found"));

    // Check authorisation: confirmed booking OR driver.
    boolean isDriver = journey.getDriver().getUser().getId().equals(userId);
    boolean isPassenger = bookingRepository.findAll().stream().anyMatch(b ->
        b.getJourney().getId().equals(journeyId)
        && b.getPassenger().getId().equals(userId)
        && b.getStatus() == BookingStatus.CONFIRMED);

    if (!isDriver && !isPassenger) {
      throw new BadRequestException("No confirmed booking for this journey");
    }

    return gpsRepository.findLatestByJourneyId(journeyId)
        .map(fix -> {
          long lastUpdatedMs = fix.getRecordedAt().toEpochMilli();
          boolean fresh = fix.getRecordedAt()
              .isAfter(Instant.now().minus(FRESHNESS_SECONDS, ChronoUnit.SECONDS));
          return new TrackingState(
              journeyId.toString(),
              journey.getStatus().name(),
              fix.getLatitude(),
              fix.getLongitude(),
              fix.getHeading(),
              fix.getSpeed(),
              lastUpdatedMs,
              fresh);
        })
        .orElse(new TrackingState(
            journeyId.toString(),
            journey.getStatus().name(),
            null, null, null, null, 0, false));
  }
}
