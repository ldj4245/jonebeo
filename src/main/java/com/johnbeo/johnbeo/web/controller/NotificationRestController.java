package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.domain.notification.dto.NotificationResponse;
import com.johnbeo.johnbeo.domain.notification.service.NotificationService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (principal == null) {
            return ResponseEntity.ok(PageResponse.empty());
        }

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> notifications = PageResponse.from(
            notificationService.getNotifications(principal.getId(), pageable)
                .map(NotificationResponse::from)
        );

        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }

        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "10") int limit
    ) {
        if (principal == null) {
            return ResponseEntity.ok(List.of());
        }

        List<NotificationResponse> notifications = notificationService
            .getUnreadNotifications(principal.getId(), limit)
            .stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 읽음 처리
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }
}

