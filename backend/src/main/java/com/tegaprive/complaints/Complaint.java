package com.tegaprive.complaints;

import java.time.Instant;
import java.util.UUID;

import com.tegaprive.common.BaseEntity;
import com.tegaprive.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter @Setter @NoArgsConstructor
public class Complaint extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "complainant_id", nullable = false)
  private User complainant;

  @Column(name = "subject_type", nullable = false)
  private String subjectType;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private String status = "OPEN";

  @Column(columnDefinition = "text")
  private String resolution;

  @Column(name = "assigned_to")
  private UUID assignedTo;

  @Column(name = "resolved_at")
  private Instant resolvedAt;
}
