package com.johnbeo.johnbeo.domain.member.repository;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.entity.MemberActivity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberActivityRepository extends JpaRepository<MemberActivity, Long> {

    Optional<MemberActivity> findByMember(Member member);

    Optional<MemberActivity> findByMemberId(Long memberId);

    // 레벨 순위 (레벨이 같으면 경험치 순)
    @Query("SELECT ma FROM MemberActivity ma ORDER BY ma.level DESC, ma.experiencePoints DESC")
    Page<MemberActivity> findAllByOrderByLevelDescExperiencePointsDesc(Pageable pageable);

    // 경험치 순위
    @Query("SELECT ma FROM MemberActivity ma ORDER BY ma.experiencePoints DESC")
    Page<MemberActivity> findAllByOrderByExperiencePointsDesc(Pageable pageable);

    // 총 게시글 순위
    @Query("SELECT ma FROM MemberActivity ma ORDER BY ma.totalPosts DESC")
    Page<MemberActivity> findAllByOrderByTotalPostsDesc(Pageable pageable);

    // 총 추천 순위
    @Query("SELECT ma FROM MemberActivity ma ORDER BY ma.totalUpvotes DESC")
    Page<MemberActivity> findAllByOrderByTotalUpvotesDesc(Pageable pageable);
}

