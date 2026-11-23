package com.tegaprive.payments;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlatformSetting {

  @Id
  @Column(nullable = false)
  private String key;

  @Column(name = "value", nullable = false, columnDefinition = "text")
  private String value;

  @Column
  private String description;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "updated_by")
  private UUID updatedBy;
}
