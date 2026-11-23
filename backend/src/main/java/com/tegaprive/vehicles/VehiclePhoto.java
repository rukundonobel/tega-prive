package com.tegaprive.vehicles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;

@Entity
@Table(name = "vehicle_photos")
@Getter
@Setter
@NoArgsConstructor
public class VehiclePhoto extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(nullable = false)
  private String url;

  @Column(name = "public_id")
  private String publicId;

  @Column(nullable = false)
  private int position = 0;

  @Column(name = "is_primary", nullable = false)
  private boolean primary = false;
}
