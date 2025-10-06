package com.johnbeo.johnbeo.domain.feed.service;

import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.feed.dto.HomeFeedDto;
import com.johnbeo.johnbeo.domain.feed.dto.HomePostCard;
import com.johnbeo.johnbeo.domain.feed.dto.RecentCommentCard;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import com.johnbeo.johnbeo.domain.vote.repository.VoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeFeedService {

    private static final int DEFAULT_CARD_SIZE = 6;
    private static final int BOARD_FEED_SIZE = 5;
    private static final int RECENT_COMMENT_SIZE = 6;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    public HomeFeedDto loadHomeFeed() {
        Instant now = Instant.now(clock);
        Instant trendingSince = now.minus(2, ChronoUnit.DAYS);

        List<Post> trendingPosts = postRepository.findTrendingSince(trendingSince, page(DEFAULT_CARD_SIZE));
        List<Post> freshPosts = postRepository.findRecent(page(DEFAULT_CARD_SIZE));
        Map<BoardType, List<HomePostCard>> boardFeeds = buildBoardFeeds();
        List<Comment> latestComments = commentRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_COMMENT_SIZE))
            .getContent();

        return new HomeFeedDto(
            toPostCards(trendingPosts),
            toPostCards(freshPosts),
            boardFeeds,
            toRecentCommentCards(latestComments)
        );
    }

    private Map<BoardType, List<HomePostCard>> buildBoardFeeds() {
        Map<BoardType, List<HomePostCard>> feeds = new EnumMap<>(BoardType.class);
        Pageable pageable = page(BOARD_FEED_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        for (BoardType type : BoardType.values()) {
            List<Post> posts = postRepository.findByBoard_Type(type, pageable).getContent();
            if (!posts.isEmpty()) {
                feeds.put(type, toPostCards(posts));
            }
        }
        return feeds;
    }

    private List<HomePostCard> toPostCards(List<Post> posts) {
        return posts.stream()
            .map(this::toPostCard)
            .collect(Collectors.toList());
    }

    private HomePostCard toPostCard(Post post) {
        String boardName = post.getBoard() != null ? post.getBoard().getName() : "미분류";
        String boardSlug = post.getBoard() != null ? post.getBoard().getSlug() : "";
        String authorNickname = post.getAuthor() != null ? post.getAuthor().getNickname() : "익명";
        long commentCount = commentRepository.countByPostId(post.getId());
        long voteScore = voteRepository.sumVoteScore(post.getId(), VoteTargetType.POST);
        return new HomePostCard(
            post.getId(),
            post.getTitle(),
            boardName,
            boardSlug,
            authorNickname,
            post.getCreatedAt(),
            post.getViewCount(),
            commentCount,
            voteScore
        );
    }

    private List<RecentCommentCard> toRecentCommentCards(List<Comment> comments) {
        return comments.stream()
            .filter(Objects::nonNull)
            .map(this::toRecentCommentCard)
            .collect(Collectors.toList());
    }

    private RecentCommentCard toRecentCommentCard(Comment comment) {
        String authorNickname = comment.getAuthor() != null ? comment.getAuthor().getNickname() : "익명";
        Post post = comment.getPost();
        String boardName = post != null && post.getBoard() != null ? post.getBoard().getName() : "게시판 미지정";
        String boardSlug = post != null && post.getBoard() != null ? post.getBoard().getSlug() : "";
        Long postId = post != null ? post.getId() : null;
        String postTitle = post != null ? post.getTitle() : "삭제된 게시글";
        return new RecentCommentCard(
            comment.getId(),
            snippet(comment.getContent(), 120),
            comment.getCreatedAt(),
            postId,
            postTitle,
            boardName,
            boardSlug,
            authorNickname
        );
    }

    private Pageable page(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "viewCount"));
    }

    private Pageable page(int size, Sort sort) {
        return PageRequest.of(0, size, sort);
    }

    private String snippet(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
