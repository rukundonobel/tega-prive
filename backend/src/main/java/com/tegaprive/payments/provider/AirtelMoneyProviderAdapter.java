package com.tegaprive.payments.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.tegaprive.payments.PaymentProperties;

/**
 * Airtel Money adapter — Rwanda sandbox.
 *
 * Required env vars:
 *   TP_AIRTEL_API_BASE_URL       (default: https://openapiuat.airtel.africa)
 *   TP_AIRTEL_CLIENT_ID          (OAuth2 client id from Airtel developer portal)
 *   TP_AIRTEL_CLIENT_SECRET      (OAuth2 client secret)
 *   TP_AIRTEL_CALLBACK_HOST      (public host for webhook callbacks)
 *
 * Full Airtel API: https://developers.airtel.africa/documentation
 */
@Component
public class AirtelMoneyProviderAdapter implements PaymentProviderPort {

  private static final Logger log = LoggerFactory.getLogger(AirtelMoneyProviderAdapter.class);

  private final PaymentProperties.Airtel config;

  public AirtelMoneyProviderAdapter(PaymentProperties props) {
    this.config = props.airtel();
  }

  @Override
  public String providerName() {
    return "AIRTEL";
  }

  @Override
  public boolean isConfigured() {
    return config != null
        && config.clientId() != null && !config.clientId().isBlank()
        && config.clientSecret() != null && !config.clientSecret().isBlank();
  }

  @Override
  public InitiateResult initiate(InitiateRequest request) {
    if (!isConfigured()) {
      throw new IllegalStateException(
          "Airtel Money is not configured. Set TP_AIRTEL_CLIENT_ID and TP_AIRTEL_CLIENT_SECRET. "
              + "See documentation/architecture.md §11.");
    }
    // Production: POST /merchant/v2/payments/
    log.info("[Airtel] Would initiate payment reference={} phone={} amount={}",
        request.internalReference(), request.phoneNumber(), request.amount());
    String payload = String.format(
        "{\"reference\":\"%s\",\"status\":\"PENDING\",\"note\":\"Airtel sandbox - awaiting real credentials\"}",
        request.internalReference());
    return new InitiateResult(request.internalReference(), "PENDING", payload);
  }

  @Override
  public VerifyResult verify(String providerReference) {
    if (!isConfigured()) {
      return new VerifyResult("PENDING", providerReference, "{\"note\":\"not configured\"}");
    }
    // Production: GET /standard/v1/payments/{id}
    log.info("[Airtel] Would verify providerReference={}", providerReference);
    return new VerifyResult("PENDING", providerReference, "{\"status\":\"PENDING\",\"note\":\"sandbox stub\"}");
  }
}
