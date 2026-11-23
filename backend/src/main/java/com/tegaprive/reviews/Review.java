package com.tegaprive.reviews;

import com.tegaprive.bookings.Booking;
import com.tegaprive.common.BaseEntity;
import com.tegaprive.drivers.Driver;
import com.tegaprive.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor
public class Review extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewer_id", nullable = false)
  private User reviewer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @Column(nullable = false)
  private int rating;

  @Column(columnDefinition = "text")
  private String comment;

  @Column(nullable = false)
  private boolean moderated = false;

  @Column(nullable = false)
  private boolean hidden = false;
}
