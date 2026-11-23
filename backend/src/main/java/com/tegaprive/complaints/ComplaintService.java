package com.tegaprive.complaints;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.complaints.dto.ComplaintDto;
import com.tegaprive.complaints.dto.ComplaintRequest;
import com.tegaprive.complaints.dto.ResolveRequest;
import com.tegaprive.users.UserRepository;

@Service
public class ComplaintService {

  private final ComplaintRepository repo;
  private final UserRepository userRepo;

  public ComplaintService(ComplaintRepository repo, UserRepository userRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
  }

  @Transactional
  public ComplaintDto file(UUID userId, ComplaintRequest request) {
    var user = userRepo.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Complaint c = new Complaint();
    c.setComplainant(user);
    c.setSubjectType(request.subjectType());
    c.setSubjectId(request.subjectId());
    c.setCategory(request.category());
    c.setDescription(request.description());
    c.setStatus("OPEN");
    repo.save(c);
    return toDto(c);
  }

  @Transactional(readOnly = true)
  public List<ComplaintDto> myComplaints(UUID userId) {
    return repo.findByComplainantIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ComplaintDto> openComplaints() {
    return repo.findByStatusOrderByCreatedAtDesc("OPEN").stream()
        .map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ComplaintDto> allComplaints() {
    return repo.findAll().stream()
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .map(this::toDto).toList();
  }

  @Transactional
  public ComplaintDto resolve(UUID complaintId, ResolveRequest request, UUID adminId) {
    Complaint c = repo.findById(complaintId)
        .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    c.setStatus("RESOLVED");
    c.setResolution(request.resolution());
    c.setAssignedTo(adminId);
    c.setResolvedAt(Instant.now());
    repo.save(c);
    return toDto(c);
  }

  private ComplaintDto toDto(Complaint c) {
    return new ComplaintDto(
        c.getId().toString(),
        c.getComplainant().getFirstName() + " " + c.getComplainant().getLastName(),
        c.getComplainant().getEmail(),
        c.getSubjectType(),
        c.getSubjectId().toString(),
        c.getCategory(),
        c.getDescription(),
        c.getStatus(),
        c.getResolution(),
        c.getCreatedAt());
  }
}
