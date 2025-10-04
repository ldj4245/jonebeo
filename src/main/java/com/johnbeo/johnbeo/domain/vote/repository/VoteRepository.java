package com.johnbeo.johnbeo.domain.vote.repository;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.vote.entity.Vote;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByMemberAndTargetIdAndTargetType(Member member, Long targetId, VoteTargetType targetType);

    long countByTargetIdAndTargetTypeAndValue(Long targetId, VoteTargetType targetType, int value);
}
