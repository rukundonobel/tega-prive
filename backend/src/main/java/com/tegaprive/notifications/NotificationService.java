package com.tegaprive.notifications;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tegaprive.common.exception.ResourceNotFoundException;
import com.tegaprive.notifications.dto.NotificationDto;
import com.tegaprive.users.User;
import com.tegaprive.users.UserRepository;

@Service
public class NotificationService {

  private final NotificationRepository repo;
  private final UserRepository userRepository;

  public NotificationService(NotificationRepository repo, UserRepository userRepository) {
    this.repo = repo;
    this.userRepository = userRepository;
  }

  /** Creates and persists an in-app notification for the given user. */
  @Transactional
  public void send(UUID userId, String type, String title, String body, String payload) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Notification n = new Notification();
    n.setUser(user);
    n.setType(type);
    n.setTitle(title);
    n.setBody(body);
    n.setPayload(payload);
    repo.save(n);
    // FCM push: add here when Firebase credentials are configured.
  }

  @Transactional(readOnly = true)
  public List<NotificationDto> myNotifications(UUID userId) {
    return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public long unreadCount(UUID userId) {
    return repo.countUnread(userId);
  }

  @Transactional
  public void markAllRead(UUID userId) {
    repo.markAllRead(userId);
  }

  private NotificationDto toDto(Notification n) {
    return new NotificationDto(
        n.getId().toString(),
        n.getType(),
        n.getTitle(),
        n.getBody(),
        n.getReadAt() == null,
        n.getCreatedAt());
  }
}
