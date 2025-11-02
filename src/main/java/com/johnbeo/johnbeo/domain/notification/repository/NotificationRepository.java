package com.johnbeo.johnbeo.domain.notification.repository;

import com.johnbeo.johnbeo.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientId(Long recipientId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(Long recipientId);
}

