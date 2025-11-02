package com.johnbeo.johnbeo.domain.member.service;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.entity.MemberActivity;
import com.johnbeo.johnbeo.domain.member.repository.MemberActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private static final long EXP_POST_CREATED = 10L;
    private static final long EXP_COMMENT_CREATED = 5L;
    private static final long EXP_UPVOTE_RECEIVED = 2L;

    private final MemberActivityRepository activityRepository;

    /**
     * 회원의 활동 정보 조회 (없으면 생성)
     */
    @Transactional
    public MemberActivity getOrCreateActivity(Member member) {
        return activityRepository.findByMember(member)
            .orElseGet(() -> {
                MemberActivity activity = MemberActivity.builder()
                    .member(member)
                    .build();
                return activityRepository.save(activity);
            });
    }

    /**
     * 회원 ID로 활동 정보 조회
     */
    public MemberActivity getActivityByMemberId(Long memberId) {
        return activityRepository.findByMemberId(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원 활동 정보를 찾을 수 없습니다: " + memberId));
    }

    /**
     * 게시글 작성 시 경험치 증가
     */
    @Transactional
    public void onPostCreated(Member author) {
        MemberActivity activity = getOrCreateActivity(author);
        activity.addExperiencePoints(EXP_POST_CREATED);
        activity.incrementTotalPosts();
        
        log.debug("게시글 작성으로 경험치 증가 - 회원: {}, 경험치: +{}, 총 경험치: {}, 레벨: {}",
            author.getUsername(), EXP_POST_CREATED, activity.getExperiencePoints(), activity.getLevel());
    }

    /**
     * 댓글 작성 시 경험치 증가
     */
    @Transactional
    public void onCommentCreated(Member author) {
        MemberActivity activity = getOrCreateActivity(author);
        activity.addExperiencePoints(EXP_COMMENT_CREATED);
        activity.incrementTotalComments();
        
        log.debug("댓글 작성으로 경험치 증가 - 회원: {}, 경험치: +{}, 총 경험치: {}, 레벨: {}",
            author.getUsername(), EXP_COMMENT_CREATED, activity.getExperiencePoints(), activity.getLevel());
    }

    /**
     * 추천 받았을 때 경험치 증가
     */
    @Transactional
    public void onUpvoteReceived(Member recipient) {
        MemberActivity activity = getOrCreateActivity(recipient);
        activity.addExperiencePoints(EXP_UPVOTE_RECEIVED);
        activity.incrementTotalUpvotes();
        
        log.debug("추천으로 경험치 증가 - 회원: {}, 경험치: +{}, 총 경험치: {}, 레벨: {}",
            recipient.getUsername(), EXP_UPVOTE_RECEIVED, activity.getExperiencePoints(), activity.getLevel());
    }

    /**
     * 비추천 받았을 때 통계 증가
     */
    @Transactional
    public void onDownvoteReceived(Member recipient) {
        MemberActivity activity = getOrCreateActivity(recipient);
        activity.incrementTotalDownvotes();
    }

    /**
     * 레벨 순위 조회
     */
    public Page<MemberActivity> getLevelRanking(Pageable pageable) {
        return activityRepository.findAllByOrderByLevelDescExperiencePointsDesc(pageable);
    }

    /**
     * 경험치 순위 조회
     */
    public Page<MemberActivity> getExperienceRanking(Pageable pageable) {
        return activityRepository.findAllByOrderByExperiencePointsDesc(pageable);
    }

    /**
     * 게시글 순위 조회
     */
    public Page<MemberActivity> getPostRanking(Pageable pageable) {
        return activityRepository.findAllByOrderByTotalPostsDesc(pageable);
    }

    /**
     * 추천 순위 조회
     */
    public Page<MemberActivity> getUpvoteRanking(Pageable pageable) {
        return activityRepository.findAllByOrderByTotalUpvotesDesc(pageable);
    }
}

