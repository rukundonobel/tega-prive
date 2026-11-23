package com.tegaprive.commissions;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionTransactionRepository extends JpaRepository<CommissionTransaction, UUID> {

  Optional<CommissionTransaction> findByBookingId(UUID bookingId);
}
