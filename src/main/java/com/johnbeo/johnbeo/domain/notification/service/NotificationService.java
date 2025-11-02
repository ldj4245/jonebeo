package com.johnbeo.johnbeo.domain.notification.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.notification.entity.Notification;
import com.johnbeo.johnbeo.domain.notification.model.NotificationType;
import com.johnbeo.johnbeo.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 생성
     */
    @Transactional
    @CacheEvict(value = "notificationCount", key = "#recipient.id")
    public void createNotification(Member recipient, NotificationType type, Long targetId, String message) {
        Notification notification = Notification.builder()
            .recipient(recipient)
            .type(type)
            .targetId(targetId)
            .message(message)
            .build();
        
        notificationRepository.save(notification);
        log.debug("알림 생성 - 수신자: {}, 타입: {}, 메시지: {}", recipient.getNickname(), type, message);
    }

    /**
     * 사용자의 알림 목록 조회
     */
    public Page<Notification> getNotifications(Long memberId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberId, pageable);
    }

    /**
     * 읽지 않은 알림 개수 조회 (캐싱)
     */
    @Cacheable(value = "notificationCount", key = "#memberId")
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(memberId);
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    public List<Notification> getUnreadNotifications(Long memberId, int limit) {
        return notificationRepository.findUnreadByRecipientId(memberId, PageRequest.of(0, limit));
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    @CacheEvict(value = "notificationCount", key = "#memberId")
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다: " + notificationId));
        
        if (!notification.getRecipient().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 처리할 수 있습니다.");
        }
        
        notification.markAsRead();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    @CacheEvict(value = "notificationCount", key = "#memberId")
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsReadByRecipientId(memberId);
    }
}

