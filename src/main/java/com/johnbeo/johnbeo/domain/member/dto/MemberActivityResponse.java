package com.johnbeo.johnbeo.domain.member.dto;

import com.johnbeo.johnbeo.domain.member.entity.MemberActivity;
import java.time.Instant;

public record MemberActivityResponse(
    Long id,
    String memberNickname,
    Integer level,
    String levelTier,
    Long experiencePoints,
    Long experienceToNextLevel,
    Integer experienceProgress,
    Long totalPosts,
    Long totalComments,
    Long totalUpvotes,
    Long totalDownvotes,
    Instant createdAt
) {
    public static MemberActivityResponse from(MemberActivity activity) {
        return new MemberActivityResponse(
            activity.getId(),
            activity.getMember().getNickname(),
            activity.getLevel(),
            activity.getLevelTier(),
            activity.getExperiencePoints(),
            activity.getExperienceToNextLevel(),
            activity.getExperienceProgress(),
            activity.getTotalPosts(),
            activity.getTotalComments(),
            activity.getTotalUpvotes(),
            activity.getTotalDownvotes(),
            activity.getCreatedAt()
        );
    }
}

