package com.johnbeo.johnbeo.domain.member.event;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.member.service.ActivityService;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.vote.entity.Vote;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventListener {

    private final ActivityService activityService;

    /**
     * 게시글 생성 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        try {
            Post post = event.getPost();
            activityService.onPostCreated(post.getAuthor());
        } catch (Exception e) {
            log.error("게시글 생성 이벤트 처리 실패", e);
        }
    }

    /**
     * 댓글 생성 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            Comment comment = event.getComment();
            activityService.onCommentCreated(comment.getAuthor());
        } catch (Exception e) {
            log.error("댓글 생성 이벤트 처리 실패", e);
        }
    }

    /**
     * 투표 생성 이벤트 처리 (추천만 경험치 증가)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteCreated(VoteCreatedEvent event) {
        try {
            Vote vote = event.getVote();
            
            // 추천(UP)인 경우만 경험치 증가
            if (!vote.isUpvote()) {
                return;
            }

            // 게시글 추천
            if (vote.getTargetType() == VoteTargetType.POST) {
                Post post = event.getPost();
                if (post != null) {
                    activityService.onUpvoteReceived(post.getAuthor());
                }
            }
            // 댓글 추천
            else if (vote.getTargetType() == VoteTargetType.COMMENT) {
                Comment comment = event.getComment();
                if (comment != null) {
                    activityService.onUpvoteReceived(comment.getAuthor());
                }
            }
        } catch (Exception e) {
            log.error("투표 생성 이벤트 처리 실패", e);
        }
    }
}

