package com.tegaprive.notifications.dto;

import java.time.Instant;

public record NotificationDto(
    String id,
    String type,
    String title,
    String body,
    boolean unread,
    Instant createdAt) {
}
