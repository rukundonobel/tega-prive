package com.tegaprive.journeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tegaprive.common.BaseEntity;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column
  private String region;

  @Column(nullable = false)
  private String country = "Rwanda";

  @Column(name = "is_active", nullable = false)
  private boolean active = true;
}
