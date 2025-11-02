package com.johnbeo.johnbeo.domain.notification.model;

public enum NotificationType {
    COMMENT,    // 내 게시글에 댓글
    REPLY,      // 내 댓글에 답글
    UPVOTE,     // 내 게시글/댓글에 추천
    MENTION,    // 멘션
    SYSTEM      // 시스템 알림
}

