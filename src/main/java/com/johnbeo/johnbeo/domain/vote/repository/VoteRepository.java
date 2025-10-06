package com.johnbeo.johnbeo.domain.vote.repository;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.vote.entity.Vote;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByMemberAndTargetIdAndTargetType(Member member, Long targetId, VoteTargetType targetType);

    long countByTargetIdAndTargetTypeAndValue(Long targetId, VoteTargetType targetType, int value);

    @Query("SELECT COALESCE(SUM(v.value), 0) FROM Vote v WHERE v.targetId = :targetId AND v.targetType = :targetType")
    long sumVoteScore(@Param("targetId") Long targetId, @Param("targetType") VoteTargetType targetType);
}
