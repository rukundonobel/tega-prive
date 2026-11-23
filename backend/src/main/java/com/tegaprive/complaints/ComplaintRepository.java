package com.tegaprive.complaints;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

  List<Complaint> findByStatusOrderByCreatedAtDesc(String status);

  List<Complaint> findByComplainantIdOrderByCreatedAtDesc(UUID complainantId);

  long countByStatus(String status);
}
