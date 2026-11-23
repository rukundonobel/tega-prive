package com.tegaprive.notifications;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.tegaprive.common.ApiResponse;
import com.tegaprive.notifications.dto.NotificationDto;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<NotificationDto>> list(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(service.myNotifications(userId));
  }

  @GetMapping("/unread-count")
  public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UUID userId) {
    return ApiResponse.ok(service.unreadCount(userId));
  }

  @PostMapping("/mark-read")
  public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UUID userId) {
    service.markAllRead(userId);
    return ApiResponse.ok(null, "All notifications marked as read");
  }
}
