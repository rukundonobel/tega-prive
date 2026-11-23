package com.tegaprive.commissions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.bookings.Booking;
import com.tegaprive.bookings.BookingStatus;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ConflictException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.payments.Payment;

/**
 * Records immutable commission transactions and driver earnings for every paid booking.
 *
 * Rules are always taken from the LATEST commission rule row (most recent effectiveFrom).
 * The rule that applied to a booking is pinned in the commission_transactions record,
 * so retroactive changes to rules never affect historical entries.
 *
 * Example (default rule: 10% driver commission, 3% passenger fee):
 *   grossFare           = 8,000 RWF
 *   driverCommission    = 800 RWF  (goes to platform)
 *   passengerServiceFee = 240 RWF  (already collected from passenger via totalAmount)
 *   driverNet           = 7,200 RWF
 *   platformRevenue     = 1,040 RWF
 */
@Service
public class CommissionEngine {

  private final CommissionRuleRepository ruleRepository;
  private final CommissionTransactionRepository txRepository;
  private final DriverEarningRepository earningRepository;

  public CommissionEngine(CommissionRuleRepository ruleRepository,
      CommissionTransactionRepository txRepository,
      DriverEarningRepository earningRepository) {
    this.ruleRepository = ruleRepository;
    this.txRepository = txRepository;
    this.earningRepository = earningRepository;
  }

  /**
   * Called when a payment is confirmed successful.
   * Creates a CommissionTransaction + DriverEarning row (idempotent — skips if already recorded).
   */
  @Transactional
  public CommissionResult settle(Booking booking, Payment payment) {
    // Idempotent: if already settled, return the existing result.
    return txRepository.findByBookingId(booking.getId())
        .map(tx -> resultFrom(tx))
        .orElseGet(() -> doSettle(booking, payment));
  }

  private CommissionResult doSettle(Booking booking, Payment payment) {
    CommissionRule rule = ruleRepository.findLatest()
        .orElseThrow(() -> new IllegalStateException("No commission rule configured"));

    BigDecimal grossFare = booking.getGrossFare();

    BigDecimal driverCommission = grossFare
        .multiply(rule.getDriverCommissionPct())
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal passengerServiceFee = booking.getServiceFee(); // already computed at booking time

    BigDecimal driverNet = grossFare.subtract(driverCommission);
    BigDecimal platformRevenue = driverCommission.add(passengerServiceFee);

    CommissionTransaction tx = new CommissionTransaction();
    tx.setBooking(booking);
    tx.setPayment(payment);
    tx.setRule(rule);
    tx.setGrossFare(grossFare);
    tx.setDriverCommission(driverCommission);
    tx.setPassengerServiceFee(passengerServiceFee);
    tx.setDriverNet(driverNet);
    tx.setPlatformRevenue(platformRevenue);
    txRepository.save(tx);

    // Record driver earning
    DriverEarning earning = new DriverEarning();
    earning.setDriver(booking.getJourney().getDriver());
    earning.setBooking(booking);
    earning.setGrossFare(grossFare);
    earning.setCommission(driverCommission);
    earning.setNetEarnings(driverNet);
    earning.setSettlementStatus("PENDING");
    earningRepository.save(earning);

    return resultFrom(tx);
  }

  private CommissionResult resultFrom(CommissionTransaction tx) {
    return new CommissionResult(
        tx.getGrossFare(), tx.getDriverCommission(),
        tx.getPassengerServiceFee(), tx.getDriverNet(), tx.getPlatformRevenue());
  }

  public record CommissionResult(
      BigDecimal grossFare,
      BigDecimal driverCommission,
      BigDecimal passengerServiceFee,
      BigDecimal driverNet,
      BigDecimal platformRevenue) {
  }
}
