package com.tegaprive.config;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tegaprive.payments.PlatformSettingsRepository;

/**
 * Runtime access to key/value platform settings stored in DB.
 * Falls back to safe defaults when a key is absent.
 */
@Component
public class PlatformSettings {

  private final PlatformSettingsRepository repo;

  public PlatformSettings(PlatformSettingsRepository repo) {
    this.repo = repo;
  }

  public boolean cashPaymentsEnabled() {
    return getBool("cash_payments_enabled", false);
  }

  public boolean isInstantConfirmation() {
    return "INSTANT".equalsIgnoreCase(get("booking_workflow", "INSTANT"));
  }

  public long maxCashCommissionDebt() {
    return getLong("max_cash_commission_debt", 50_000L);
  }

  public int paymentExpiryMinutes() {
    return (int) getLong("payment_expiry_minutes", 30L);
  }

  public String get(String key, String defaultValue) {
    return Optional.ofNullable(repo.findByKey(key)).map(s -> s.getValue()).orElse(defaultValue);
  }

  private boolean getBool(String key, boolean def) {
    return Boolean.parseBoolean(get(key, String.valueOf(def)));
  }

  private long getLong(String key, long def) {
    try {
      return Long.parseLong(get(key, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }
}
