package com.tegaprive.drivers;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;

@Entity
@Table(name = "driver_documents")
@Getter
@Setter
@NoArgsConstructor
public class DriverDocument extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DriverDocumentType type;

  @Column(nullable = false)
  private String url;

  @Column(name = "public_id")
  private String publicId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentStatus status = DocumentStatus.PENDING;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;
}
