package com.johnbeo.johnbeo.domain.vote.dto;

import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;

public record VoteSummaryResponse(
    Long targetId,
    VoteTargetType targetType,
    long upVotes,
    long downVotes,
    long score,
    Integer userVote
) {
}
