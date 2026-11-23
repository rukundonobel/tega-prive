package com.tegaprive.payments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByInternalReference(String internalReference);

  Optional<Payment> findByProviderReference(String providerReference);

  List<Payment> findByBookingId(UUID bookingId);

  boolean existsByInternalReference(String internalReference);
}
