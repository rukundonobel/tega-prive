package com.tegaprive.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tp.payment")
public record PaymentProperties(
    @DefaultValue Mtn mtn,
    @DefaultValue Airtel airtel,
    @DefaultValue("true") boolean devBypass) {

  public record Mtn(
      @DefaultValue("https://sandbox.momodeveloper.mtn.com") String apiBaseUrl,
      @DefaultValue("") String subscriptionKey,
      @DefaultValue("") String apiUser,
      @DefaultValue("") String apiKey,
      @DefaultValue("") String callbackHost,
      @DefaultValue("sandbox") String targetEnvironment) {
  }

  public record Airtel(
      @DefaultValue("https://openapiuat.airtel.africa") String apiBaseUrl,
      @DefaultValue("") String clientId,
      @DefaultValue("") String clientSecret,
      @DefaultValue("") String callbackHost) {
  }
}
