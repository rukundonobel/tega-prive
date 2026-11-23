package com.tegaprive.payments.provider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.payments.PaymentProvider;

/**
 * Selects the right PaymentProviderPort implementation for a given PaymentProvider enum.
 * Adding a new gateway = inject a new adapter here; nothing else changes.
 */
@Component
public class PaymentGateway {

  private final Map<String, PaymentProviderPort> adapters;

  public PaymentGateway(List<PaymentProviderPort> all) {
    this.adapters = all.stream()
        .collect(Collectors.toMap(PaymentProviderPort::providerName, p -> p));
  }

  public PaymentProviderPort get(PaymentProvider provider) {
    if (provider == PaymentProvider.CASH) {
      throw new BadRequestException("Cash payments are handled directly — no provider required.");
    }
    PaymentProviderPort port = adapters.get(provider.name());
    if (port == null) {
      throw new BadRequestException("Unsupported payment provider: " + provider);
    }
    return port;
  }
}
