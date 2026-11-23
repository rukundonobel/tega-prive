package com.tegaprive.commissions;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, UUID> {

  /** Returns the most recently effective rule at the time of booking. */
  @Query("SELECT r FROM CommissionRule r ORDER BY r.effectiveFrom DESC LIMIT 1")
  Optional<CommissionRule> findLatest();
}
