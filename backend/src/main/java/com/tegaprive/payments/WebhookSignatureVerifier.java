package com.tegaprive.payments;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifies provider webhook signatures using HMAC-SHA256.
 *
 * MTN MoMo:  signature is in header 'X-Callback-Signature'
 *            secret is TP_MTN_WEBHOOK_SECRET
 *
 * Airtel:    signature is in header 'Signature'
 *            secret is TP_AIRTEL_WEBHOOK_SECRET
 *
 * When secrets are not configured, all webhooks are accepted (dev mode).
 * In production, always set the webhook secrets and set tp.payment.dev-bypass=false.
 *
 * Full MTN signature spec: https://momodeveloper.mtn.com/api-documentation/security
 * Full Airtel signature spec: https://developers.airtel.africa/documentation/security
 */
@Component
public class WebhookSignatureVerifier {

  private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
  private static final String ALGO = "HmacSHA256";

  private final PaymentProperties props;

  public WebhookSignatureVerifier(PaymentProperties props) {
    this.props = props;
  }

  /**
   * Verifies the HMAC-SHA256 signature of a webhook payload.
   *
   * @param provider        "MTN_MOMO" or "AIRTEL"
   * @param payload         raw request body as received
   * @param signatureHeader value of the signature header
   * @return true if the signature is valid (or if the secret is not configured)
   */
  public boolean verify(String provider, String payload, String signatureHeader) {
    String secret = resolveSecret(provider);

    if (secret == null || secret.isBlank()) {
      // No secret configured — accept in dev, log a warning
      log.warn("Webhook signature verification skipped for provider={} (no secret configured). "
          + "Set TP_{}_WEBHOOK_SECRET in production.", provider, provider.replace("_MOMO", ""));
      return true;
    }

    if (signatureHeader == null || signatureHeader.isBlank()) {
      log.warn("Webhook rejected: missing signature header for provider={}", provider);
      return false;
    }

    try {
      Mac mac = Mac.getInstance(ALGO);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
      byte[] expectedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String expected = HexFormat.of().formatHex(expectedBytes);
      // Some providers prefix with "sha256="
      String actual = signatureHeader.startsWith("sha256=")
          ? signatureHeader.substring(7) : signatureHeader;
      return constantTimeEquals(expected, actual);
    } catch (Exception e) {
      log.error("Signature verification error for provider={}: {}", provider, e.getMessage());
      return false;
    }
  }

  /** Timing-safe string comparison to prevent timing attacks. */
  private boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }

  private String resolveSecret(String provider) {
    // Webhook secrets are separate from API credentials — set in env.
    return switch (provider) {
      case "MTN_MOMO" -> System.getenv("TP_MTN_WEBHOOK_SECRET");
      case "AIRTEL" -> System.getenv("TP_AIRTEL_WEBHOOK_SECRET");
      default -> null;
    };
  }
}
