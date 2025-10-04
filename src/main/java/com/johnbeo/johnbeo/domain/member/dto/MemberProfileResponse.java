package com.johnbeo.johnbeo.domain.member.dto;

import com.johnbeo.johnbeo.domain.member.model.Role;
import java.time.Instant;
import java.util.List;

public record MemberProfileResponse(
    Long id,
    String username,
    String email,
    String nickname,
    Role role,
    Instant joinedAt,
    long postCount,
    long commentCount,
    List<RecentPost> recentPosts
) {

    public record RecentPost(
        Long id,
        String title,
        String boardName,
        String boardSlug,
        Instant createdAt,
        long viewCount
    ) {
    }
}
