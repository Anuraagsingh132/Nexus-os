package com.nexusos.api.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String title,
    String message,
    boolean isRead,
    Instant createdAt
) {}
