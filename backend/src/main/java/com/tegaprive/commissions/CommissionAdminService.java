package com.tegaprive.commissions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.commissions.dto.CommissionRuleDto;
import com.tegaprive.commissions.dto.CommissionRuleRequest;
import com.tegaprive.commissions.dto.DriverEarningDto;
import com.tegaprive.commissions.dto.DriverPayoutDto;
import com.tegaprive.commissions.dto.PayoutRequest;
import com.tegaprive.common.exception.BadRequestException;
import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.drivers.DriverRepository;

@Service
public class CommissionAdminService {

  private final CommissionRuleRepository ruleRepository;
  private final DriverEarningRepository earningRepository;
  private final DriverPayoutRepository payoutRepository;
  private final DriverRepository driverRepository;

  public CommissionAdminService(CommissionRuleRepository ruleRepository,
      DriverEarningRepository earningRepository,
      DriverPayoutRepository payoutRepository,
      DriverRepository driverRepository) {
    this.ruleRepository = ruleRepository;
    this.earningRepository = earningRepository;
    this.payoutRepository = payoutRepository;
    this.driverRepository = driverRepository;
  }

  @Transactional(readOnly = true)
  public CommissionRuleDto currentRule() {
    CommissionRule rule = ruleRepository.findLatest()
        .orElseThrow(() -> new IllegalStateException("No commission rule configured"));
    return toRuleDto(rule);
  }

  @Transactional(readOnly = true)
  public List<CommissionRuleDto> ruleHistory() {
    return ruleRepository.findAll().stream()
        .sorted((a, b) -> b.getEffectiveFrom().compareTo(a.getEffectiveFrom()))
        .map(this::toRuleDto)
        .toList();
  }

  @Transactional
  public CommissionRuleDto createRule(CommissionRuleRequest request, UUID adminId) {
    CommissionRule rule = new CommissionRule();
    rule.setDriverCommissionPct(request.driverCommissionPct());
    rule.setPassengerServiceFeePct(request.passengerServiceFeePct());
    rule.setMinServiceFee(request.minServiceFee() != null ? request.minServiceFee() : BigDecimal.ZERO);
    rule.setMaxServiceFee(request.maxServiceFee());
    rule.setFixedBookingFee(request.fixedBookingFee() != null ? request.fixedBookingFee() : BigDecimal.ZERO);
    rule.setCancellationFeePct(request.cancellationFeePct() != null ? request.cancellationFeePct() : BigDecimal.ZERO);
    rule.setEffectiveFrom(Instant.now());
    rule.setCreatedBy(adminId);
    rule.setNote(request.note());
    ruleRepository.save(rule);
    return toRuleDto(rule);
  }

  @Transactional(readOnly = true)
  public List<DriverEarningDto> driverEarnings(UUID driverId) {
    return earningRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
        .map(this::toEarningDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public BigDecimal pendingEarnings(UUID driverId) {
    return earningRepository.sumPendingEarnings(driverId);
  }

  @Transactional
  public DriverPayoutDto initiateManualPayout(PayoutRequest request, UUID adminId) {
    var driver = driverRepository.findById(request.driverId())
        .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    BigDecimal pending = earningRepository.sumPendingEarnings(driver.getId());
    if (pending.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("No pending earnings to pay out");
    }
    if (request.amount().compareTo(pending) > 0) {
      throw new BadRequestException("Requested payout exceeds pending earnings of " + pending);
    }

    DriverPayout payout = new DriverPayout();
    payout.setDriver(driver);
    payout.setAmount(request.amount());
    payout.setProvider(request.provider());
    payout.setPhoneNumber(request.phoneNumber());
    payout.setStatus("PENDING");
    payout.setInitiatedBy(adminId);
    payoutRepository.save(payout);

    return toPayoutDto(payout);
  }

  @Transactional
  public DriverPayoutDto markPayoutPaid(UUID payoutId, String providerReference, UUID adminId) {
    DriverPayout payout = payoutRepository.findById(payoutId)
        .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
    if (!"PENDING".equals(payout.getStatus())) {
      throw new BadRequestException("Payout is not in PENDING status");
    }

    payout.setStatus("PAID");
    payout.setProviderReference(providerReference);
    payout.setPaidAt(Instant.now());
    payoutRepository.save(payout);

    // Mark earnings as settled
    earningRepository.findByDriverIdOrderByCreatedAtDesc(payout.getDriver().getId()).stream()
        .filter(e -> "PENDING".equals(e.getSettlementStatus()))
        .forEach(e -> {
          e.setSettlementStatus("PAID");
          e.setPayoutId(payout.getId());
          earningRepository.save(e);
        });

    return toPayoutDto(payout);
  }

  @Transactional(readOnly = true)
  public List<DriverPayoutDto> pendingPayouts() {
    return payoutRepository.findByStatus("PENDING").stream()
        .map(this::toPayoutDto)
        .toList();
  }

  // ── mappers ──

  private CommissionRuleDto toRuleDto(CommissionRule r) {
    return new CommissionRuleDto(
        r.getId().toString(),
        r.getDriverCommissionPct(),
        r.getPassengerServiceFeePct(),
        r.getMinServiceFee(),
        r.getMaxServiceFee(),
        r.getFixedBookingFee(),
        r.getCancellationFeePct(),
        r.getEffectiveFrom(),
        r.getNote());
  }

  private DriverEarningDto toEarningDto(DriverEarning e) {
    var user = e.getDriver().getUser();
    return new DriverEarningDto(
        e.getId().toString(),
        e.getDriver().getId().toString(),
        (user.getFirstName() + " " + user.getLastName()).trim(),
        e.getBooking().getId().toString(),
        e.getGrossFare(),
        e.getCommission(),
        e.getNetEarnings(),
        e.getSettlementStatus(),
        e.getCreatedAt());
  }

  private DriverPayoutDto toPayoutDto(DriverPayout p) {
    var user = p.getDriver().getUser();
    return new DriverPayoutDto(
        p.getId().toString(),
        p.getDriver().getId().toString(),
        (user.getFirstName() + " " + user.getLastName()).trim(),
        p.getAmount(),
        p.getProvider(),
        p.getPhoneNumber(),
        p.getStatus(),
        p.getPaidAt(),
        p.getCreatedAt());
  }
}
