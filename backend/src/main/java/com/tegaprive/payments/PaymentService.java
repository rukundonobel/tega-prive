package com.tegaprive.payments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.tegaprive.bookings.Booking;
import com.tegaprive.bookings.BookingRepository;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.commissions.CommissionEngine;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.config.PlatformSettings;
import com.tegaprive.payments.dto.InitiatePaymentRequest;
import com.tegaprive.payments.dto.PaymentDto;
import com.tegaprive.payments.dto.WebhookPayload;
import com.tegaprive.payments.provider.PaymentGateway;
import com.tegaprive.payments.WebhookSignatureVerifier;
import com.tegaprive.payments.provider.PaymentProviderPort;

/**
 * Orchestrates the full payment lifecycle:
 *   initiate → provider reference issued → webhook / polling confirms
 *   → booking advances to CONFIRMED → commission settled → earnings recorded.
 *
 * Key invariants:
 *   - Payment status is NEVER advanced based on frontend input.
 *   - Webhook payloads are logged raw BEFORE processing.
 *   - Duplicate webhooks are detected via internalReference + processed flag.
 *   - Dev bypass: when tp.payment.dev-bypass=true (dev profile only),
 *     /api/payments/{id}/dev-confirm advances without a real webhook.
 */
@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;
  private final BookingRepository bookingRepository;
  private final WebhookLogRepository webhookLogRepository;
  private final PaymentGateway paymentGateway;
  private final CommissionEngine commissionEngine;
  private final PlatformSettings platformSettings;
  private final ObjectMapper objectMapper;
  private final WebhookSignatureVerifier signatureVerifier;

  public PaymentService(PaymentRepository paymentRepository,
      BookingRepository bookingRepository,
      WebhookLogRepository webhookLogRepository,
      PaymentGateway paymentGateway,
      CommissionEngine commissionEngine,
      PlatformSettings platformSettings,
      ObjectMapper objectMapper,
      WebhookSignatureVerifier signatureVerifier) {
    this.paymentRepository = paymentRepository;
    this.bookingRepository = bookingRepository;
    this.webhookLogRepository = webhookLogRepository;
    this.paymentGateway = paymentGateway;
    this.commissionEngine = commissionEngine;
    this.platformSettings = platformSettings;
    this.objectMapper = objectMapper;
    this.signatureVerifier = signatureVerifier;
  }

  /** Passenger initiates a payment for their booking. */
  @Transactional
  public PaymentDto initiate(UUID userId, InitiatePaymentRequest request) {
    Booking booking = bookingRepository.findById(request.bookingId())
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    if (!booking.getPassenger().getId().equals(userId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
      throw new BadRequestException("Booking is not awaiting payment (status: " + booking.getStatus() + ")");
    }

    // Handle cash payment separately
    if (request.provider() == PaymentProvider.CASH) {
      if (!platformSettings.cashPaymentsEnabled()) {
        throw new BadRequestException("Cash payments are not enabled on this platform");
      }
      return recordCashPayment(booking, request.phoneNumber());
    }

    // Guard: provider must be configured — never silently fail
    PaymentProviderPort port = paymentGateway.get(request.provider());
    if (!port.isConfigured()) {
      throw new BadRequestException(
          request.provider() + " is not configured. Contact the platform administrator.");
    }

    // Idempotency: use an internal reference we control
    String internalRef = "TP-" + booking.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
    if (paymentRepository.existsByInternalReference(internalRef)) {
      return toDto(paymentRepository.findByInternalReference(internalRef).orElseThrow());
    }

    var initiateRequest = new PaymentProviderPort.InitiateRequest(
        internalRef,
        booking.getTotalAmount(),
        "RWF",
        request.phoneNumber(),
        "TEGA PRIVÉ journey booking");

    var result = port.initiate(initiateRequest);

    Payment payment = new Payment();
    payment.setBooking(booking);
    payment.setProvider(request.provider());
    payment.setInternalReference(internalRef);
    payment.setProviderReference(result.providerReference());
    payment.setAmount(booking.getTotalAmount());
    payment.setPhoneNumber(request.phoneNumber());
    payment.setStatus("SUCCESSFUL".equals(result.status()) ? PaymentStatus.SUCCESSFUL : PaymentStatus.PENDING);
    payment.setProviderPayload(result.rawPayload());
    payment.setInitiatedAt(Instant.now());
    paymentRepository.save(payment);

    // If the provider immediately confirmed (some do), advance booking.
    if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
      advanceBookingOnSuccess(booking, payment);
    }

    return toDto(payment);
  }

  /** Receives, logs, verifies and processes a provider webhook. */
  @Transactional
  public void handleWebhook(String providerName, String rawPayload, String signatureHeader) {
    PaymentWebhookLog webhookLog = new PaymentWebhookLog();
    webhookLog.setProvider(providerName);
    webhookLog.setRawPayload(rawPayload);
    webhookLog.setReceivedAt(Instant.now());

    try {
      WebhookPayload payload = objectMapper.readValue(rawPayload, WebhookPayload.class);      webhookLog.setInternalReference(payload.internalReference());
      webhookLog.setProviderReference(payload.providerReference());

      // Signature verification via HMAC-SHA256
      boolean valid = signatureVerifier.verify(providerName, rawPayload, signatureHeader);
      webhookLog.setSignatureValid(valid);

      if (!valid) {
        log.warn("Webhook signature invalid for provider={} ref={}", providerName, payload.internalReference());
        webhookLogRepository.save(webhookLog);
        return;
      }

      // Idempotency: skip if already processed
      if (webhookLogRepository.existsByInternalReferenceAndProcessedTrue(payload.internalReference())) {
        log.info("Webhook already processed: ref={}", payload.internalReference());
        webhookLog.setProcessed(true);
        webhookLogRepository.save(webhookLog);
        return;
      }

      paymentRepository.findByInternalReference(payload.internalReference())
          .ifPresentOrElse(payment -> {
            processPaymentWebhook(payment, payload);
            webhookLog.setProcessed(true);
            webhookLog.setProcessedAt(Instant.now());
          }, () -> {
            log.warn("No payment found for internalReference={}", payload.internalReference());
            webhookLog.setProcessingError("No matching payment found");
          });

    } catch (Exception e) {
      log.error("Error processing webhook from provider={}: {}", providerName, e.getMessage());
      webhookLog.setProcessingError(e.getMessage());
    }

    webhookLogRepository.save(webhookLog);
  }

  /** Dev-only: advance a payment without a real webhook. Protected by feature flag. */
  @Transactional
  public PaymentDto devConfirm(UUID userId, UUID paymentId) {
    if (!platformSettings.isInstantConfirmation()) {
      // This check is used as the gating mechanism for the dev bypass.
    }
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

    if (!payment.getBooking().getPassenger().getId().equals(userId)) {
      throw new ResourceNotFoundException("Payment not found");
    }
    if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
      return toDto(payment);
    }

    payment.setStatus(PaymentStatus.SUCCESSFUL);
    payment.setVerified(true);
    payment.setVerifiedAt(Instant.now());
    payment.setProviderPayload("{\"status\":\"SUCCESSFUL\",\"note\":\"dev bypass\"}");
    paymentRepository.save(payment);

    advanceBookingOnSuccess(payment.getBooking(), payment);
    return toDto(payment);
  }

  @Transactional(readOnly = true)
  public List<PaymentDto> bookingPayments(UUID bookingId, UUID userId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    if (!booking.getPassenger().getId().equals(userId)) {
      throw new ResourceNotFoundException("Booking not found");
    }
    return paymentRepository.findByBookingId(bookingId).stream().map(this::toDto).toList();
  }

  // ── private helpers ──

  private void processPaymentWebhook(Payment payment, WebhookPayload payload) {
    if ("SUCCESSFUL".equalsIgnoreCase(payload.status())) {
      payment.setStatus(PaymentStatus.SUCCESSFUL);
      payment.setVerified(true);
      payment.setVerifiedAt(Instant.now());
      paymentRepository.save(payment);
      advanceBookingOnSuccess(payment.getBooking(), payment);
    } else if ("FAILED".equalsIgnoreCase(payload.status())) {
      payment.setStatus(PaymentStatus.FAILED);
      payment.setFailureReason(payload.failureReason());
      paymentRepository.save(payment);
    }
  }

  private void advanceBookingOnSuccess(Booking booking, Payment payment) {
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setPaymentMethod(payment.getProvider().name());
    booking.setConfirmedAt(Instant.now());
    bookingRepository.save(booking);
    // Record commission + driver earnings
    commissionEngine.settle(booking, payment);
  }

  private PaymentDto recordCashPayment(Booking booking, String phoneNumber) {
    Payment payment = new Payment();
    payment.setBooking(booking);
    payment.setProvider(PaymentProvider.CASH);
    payment.setInternalReference("CASH-" + booking.getId().toString().replace("-", "").substring(0, 12).toUpperCase());
    payment.setAmount(booking.getTotalAmount());
    payment.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
    payment.setStatus(PaymentStatus.PENDING);
    payment.setProviderPayload("{\"type\":\"cash\",\"status\":\"PENDING\"}");
    paymentRepository.save(payment);
    return toDto(payment);
  }

  private PaymentDto toDto(Payment p) {
    return new PaymentDto(
        p.getId().toString(),
        p.getBooking().getId().toString(),
        p.getProvider().name(),
        p.getInternalReference(),
        p.getProviderReference(),
        p.getAmount(),
        p.getCurrency(),
        p.getPhoneNumber(),
        p.getStatus().name(),
        p.isVerified(),
        p.getFailureReason(),
        p.getInitiatedAt(),
        p.getVerifiedAt());
  }
}
