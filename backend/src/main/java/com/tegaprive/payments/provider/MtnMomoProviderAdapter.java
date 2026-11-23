package com.tegaprive.payments.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.tegaprive.payments.PaymentProperties;

/**
 * MTN Mobile Money (MoMo) adapter — Rwanda sandbox.
 *
 * Required env vars:
 *   TP_MTN_API_BASE_URL            (default: https://sandbox.momodeveloper.mtn.com)
 *   TP_MTN_SUBSCRIPTION_KEY        (Ocp-Apim-Subscription-Key for Collections API)
 *   TP_MTN_API_USER                (X-Reference-Id / API User created in sandbox)
 *   TP_MTN_API_KEY                 (API Key for the API User)
 *   TP_MTN_CALLBACK_HOST           (public host for webhook callbacks)
 *   TP_MTN_TARGET_ENVIRONMENT      (sandbox | mtnrwanda)
 *
 * Full MTN MoMo API: https://momodeveloper.mtn.com/api-documentation
 */
@Component
public class MtnMomoProviderAdapter implements PaymentProviderPort {

  private static final Logger log = LoggerFactory.getLogger(MtnMomoProviderAdapter.class);

  private final PaymentProperties.Mtn config;

  public MtnMomoProviderAdapter(PaymentProperties props) {
    this.config = props.mtn();
  }

  @Override
  public String providerName() {
    return "MTN_MOMO";
  }

  @Override
  public boolean isConfigured() {
    return config != null
        && config.subscriptionKey() != null && !config.subscriptionKey().isBlank()
        && config.apiUser() != null && !config.apiUser().isBlank()
        && config.apiKey() != null && !config.apiKey().isBlank();
  }

  @Override
  public InitiateResult initiate(InitiateRequest request) {
    if (!isConfigured()) {
      throw new IllegalStateException(
          "MTN MoMo is not configured. Set TP_MTN_SUBSCRIPTION_KEY, TP_MTN_API_USER and "
              + "TP_MTN_API_KEY. See documentation/architecture.md §11.");
    }
    // Production: POST /collection/v1_0/requesttopay
    log.info("[MTN MoMo] Would initiate payment reference={} phone={} amount={}",
        request.internalReference(), request.phoneNumber(), request.amount());
    String payload = String.format(
        "{\"referenceId\":\"%s\",\"status\":\"PENDING\",\"note\":\"MTN MoMo sandbox - awaiting real credentials\"}",
        request.internalReference());
    return new InitiateResult(request.internalReference(), "PENDING", payload);
  }

  @Override
  public VerifyResult verify(String providerReference) {
    if (!isConfigured()) {
      return new VerifyResult("PENDING", providerReference, "{\"note\":\"not configured\"}");
    }
    // Production: GET /collection/v1_0/requesttopay/{referenceId}
    log.info("[MTN MoMo] Would verify providerReference={}", providerReference);
    return new VerifyResult("PENDING", providerReference, "{\"status\":\"PENDING\",\"note\":\"sandbox stub\"}");
  }
}
