package com.tegaprive.drivers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;
import com.tegaprive.users.User;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "licence_number")
  private String licenceNumber;

  @Column(name = "licence_expiry")
  private LocalDate licenceExpiry;

  @Column(name = "profile_photo_url")
  private String profilePhotoUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @Column(name = "is_verified", nullable = false)
  private boolean verified = false;

  @Column(name = "rating_avg", nullable = false)
  private BigDecimal ratingAvg = BigDecimal.ZERO;

  @Column(name = "rating_count", nullable = false)
  private int ratingCount = 0;

  @Column(name = "commission_wallet_balance", nullable = false)
  private BigDecimal commissionWalletBalance = BigDecimal.ZERO;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "verified_by")
  private UUID verifiedBy;
}
