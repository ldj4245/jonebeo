package com.johnbeo.johnbeo.domain.notification.dto;

import com.johnbeo.johnbeo.domain.notification.entity.Notification;
import com.johnbeo.johnbeo.domain.notification.model.NotificationType;
import java.time.Instant;

public record NotificationResponse(
    Long id,
    NotificationType type,
    Long targetId,
    String message,
    Boolean isRead,
    Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTargetId(),
            notification.getMessage(),
            notification.getIsRead(),
            notification.getCreatedAt()
        );
    }
}

