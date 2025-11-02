package com.johnbeo.johnbeo.domain.trending.service;

import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.vote.repository.VoteRepository;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendingService {

    private final PostRepository postRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    /**
     * 트렌딩 게시글 조회 (24시간 기준, 캐싱)
     */
    @Cacheable(value = "trendingPosts", key = "'24h:' + #limit")
    public List<Post> getTrendingPosts24h(int limit) {
        Instant since = Instant.now().minusSeconds(24 * 3600);
        List<Post> recentPosts = postRepository.findByCreatedAtAfter(since, PageRequest.of(0, 100));
        
        return recentPosts.stream()
            .map(post -> {
                double score = calculateTrendingScore(post, 24);
                return new PostWithScore(post, score);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(limit)
            .map(pws -> pws.post)
            .collect(Collectors.toList());
    }

    /**
     * 트렌딩 게시글 조회 (7일 기준, 캐싱)
     */
    @Cacheable(value = "trendingPosts", key = "'7d:' + #limit")
    public List<Post> getTrendingPosts7d(int limit) {
        Instant since = Instant.now().minusSeconds(7 * 24 * 3600);
        List<Post> recentPosts = postRepository.findByCreatedAtAfter(since, PageRequest.of(0, 200));
        
        return recentPosts.stream()
            .map(post -> {
                double score = calculateTrendingScore(post, 7 * 24);
                return new PostWithScore(post, score);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(limit)
            .map(pws -> pws.post)
            .collect(Collectors.toList());
    }

    /**
     * 트렌딩 스코어 계산
     * 공식: (조회수 * 0.3 + 추천수 * 5 + 댓글수 * 2) / 시간경과(시간)
     */
    private double calculateTrendingScore(Post post, int hoursWindow) {
        long viewCount = post.getViewCount();
        long upvotes = voteRepository.countByTargetIdAndTargetTypeAndValue(
            post.getId(), VoteTargetType.POST, 1
        );
        long commentCount = commentRepository.countByPostId(post.getId());
        
        double hoursElapsed = Math.max(1, 
            (System.currentTimeMillis() - post.getCreatedAt().toEpochMilli()) 
            / (1000.0 * 60 * 60)
        );
        
        double rawScore = (viewCount * 0.3) + (upvotes * 5) + (commentCount * 2);
        
        // 시간 decay 적용
        double score = rawScore / Math.pow(hoursElapsed + 2, 1.5);
        
        return score;
    }

    /**
     * 5분마다 트렌딩 캐시 갱신
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void refreshTrendingCache() {
        log.info("트렌딩 캐시 갱신 시작");
        getTrendingPosts24h(10);
        getTrendingPosts7d(10);
        log.info("트렌딩 캐시 갱신 완료");
    }

    // Helper class
    private static class PostWithScore {
        Post post;
        double score;

        PostWithScore(Post post, double score) {
            this.post = post;
            this.score = score;
        }
    }
}

