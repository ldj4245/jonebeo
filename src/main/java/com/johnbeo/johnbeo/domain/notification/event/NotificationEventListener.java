package com.johnbeo.johnbeo.domain.notification.event;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.member.event.CommentCreatedEvent;
import com.johnbeo.johnbeo.domain.member.event.VoteCreatedEvent;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.notification.model.NotificationType;
import com.johnbeo.johnbeo.domain.notification.service.NotificationService;
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
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * 댓글 생성 시 알림
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            Comment comment = event.getComment();
            Post post = comment.getPost();
            Member commenter = comment.getAuthor();
            
            // 게시글 작성자에게 알림 (자기 글에 자기가 댓글 단 경우 제외)
            if (comment.getParent() == null) {
                Member postAuthor = post.getAuthor();
                if (!postAuthor.getId().equals(commenter.getId())) {
                    String message = commenter.getNickname() + "님이 \"" + 
                        truncate(post.getTitle(), 20) + "\"에 댓글을 남겼습니다.";
                    notificationService.createNotification(
                        postAuthor, 
                        NotificationType.COMMENT, 
                        post.getId(), 
                        message
                    );
                }
            }
            // 답글인 경우, 부모 댓글 작성자에게 알림
            else {
                Comment parentComment = comment.getParent();
                Member parentAuthor = parentComment.getAuthor();
                if (!parentAuthor.getId().equals(commenter.getId())) {
                    String message = commenter.getNickname() + "님이 회원님의 댓글에 답글을 남겼습니다.";
                    notificationService.createNotification(
                        parentAuthor,
                        NotificationType.REPLY,
                        post.getId(),
                        message
                    );
                }
            }
        } catch (Exception e) {
            log.error("댓글 생성 알림 처리 실패", e);
        }
    }

    /**
     * 추천 시 알림 (추천만, 비추천은 제외)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteCreated(VoteCreatedEvent event) {
        try {
            Vote vote = event.getVote();
            
            // 추천(UP)만 알림
            if (!vote.isUpvote()) {
                return;
            }

            Member voter = vote.getMember();
            
            // 게시글 추천
            if (vote.getTargetType() == VoteTargetType.POST) {
                Post post = event.getPost();
                if (post != null) {
                    Member postAuthor = post.getAuthor();
                    if (!postAuthor.getId().equals(voter.getId())) {
                        String message = voter.getNickname() + "님이 \"" + 
                            truncate(post.getTitle(), 20) + "\"을 추천했습니다.";
                        notificationService.createNotification(
                            postAuthor,
                            NotificationType.UPVOTE,
                            post.getId(),
                            message
                        );
                    }
                }
            }
            // 댓글 추천
            else if (vote.getTargetType() == VoteTargetType.COMMENT) {
                Comment comment = event.getComment();
                if (comment != null) {
                    Member commentAuthor = comment.getAuthor();
                    if (!commentAuthor.getId().equals(voter.getId())) {
                        String message = voter.getNickname() + "님이 회원님의 댓글을 추천했습니다.";
                        notificationService.createNotification(
                            commentAuthor,
                            NotificationType.UPVOTE,
                            comment.getPost().getId(),
                            message
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("추천 알림 처리 실패", e);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

