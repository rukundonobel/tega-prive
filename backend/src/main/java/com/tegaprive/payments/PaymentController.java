package com.tegaprive.payments;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.payments.dto.InitiatePaymentRequest;
import com.tegaprive.payments.dto.PaymentDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  /** Initiate a payment for a booking. Returns provider reference + internal reference. */
  @PostMapping("/initiate")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PaymentDto> initiate(
      @AuthenticationPrincipal UUID userId,
      @Valid @RequestBody InitiatePaymentRequest request) {
    return ApiResponse.ok(paymentService.initiate(userId, request));
  }

  /** Dev-only bypass: confirm payment without a real webhook. */
  @PostMapping("/{id}/dev-confirm")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PaymentDto> devConfirm(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID id) {
    return ApiResponse.ok(paymentService.devConfirm(userId, id));
  }

  /** View payments for a booking. */
  @GetMapping("/booking/{bookingId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<PaymentDto>> bookingPayments(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID bookingId) {
    return ApiResponse.ok(paymentService.bookingPayments(bookingId, userId));
  }

  /**
   * MTN MoMo webhook endpoint.
   * The webhook URL registered in MTN developer portal must be:
   *   POST {TP_MTN_CALLBACK_HOST}/api/payments/webhook/mtn
   */
  @PostMapping("/webhook/mtn")
  public ResponseEntity<Void> mtnWebhook(
      @RequestBody String rawPayload,
      @RequestHeader(value = "X-Callback-Signature", defaultValue = "") String signature) {
    paymentService.handleWebhook("MTN_MOMO", rawPayload, signature);
    return ResponseEntity.ok().build();
  }

  /**
   * Airtel Money webhook endpoint.
   * Register: POST {TP_AIRTEL_CALLBACK_HOST}/api/payments/webhook/airtel
   */
  @PostMapping("/webhook/airtel")
  public ResponseEntity<Void> airtelWebhook(
      @RequestBody String rawPayload,
      @RequestHeader(value = "Signature", defaultValue = "") String signature) {
    paymentService.handleWebhook("AIRTEL", rawPayload, signature);
    return ResponseEntity.ok().build();
  }
}
