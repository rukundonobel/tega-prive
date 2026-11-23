package com.tegaprive.notifications;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

  @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL")
  long countUnread(@Param("userId") UUID userId);

  @Modifying
  @Query("UPDATE Notification n SET n.readAt = instant WHERE n.user.id = :userId AND n.readAt IS NULL")
  void markAllRead(@Param("userId") UUID userId);
}
