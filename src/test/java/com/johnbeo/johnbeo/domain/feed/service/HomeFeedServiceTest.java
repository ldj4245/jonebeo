package com.johnbeo.johnbeo.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.feed.dto.HomeFeedDto;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.vote.model.VoteTargetType;
import com.johnbeo.johnbeo.domain.vote.repository.VoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeFeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VoteRepository voteRepository;

    private Clock clock;

    private HomeFeedService homeFeedService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-10-06T12:00:00Z"), ZoneOffset.UTC);
        homeFeedService = new HomeFeedService(postRepository, commentRepository, voteRepository, clock);
    }

    @Test
    void loadHomeFeedAggregatesSections() {
        Member author = Member.builder()
            .username("satoshi")
            .password("encoded")
            .email("satoshi@example.com")
            .nickname("사토시")
            .role(Role.USER)
            .build();
        ReflectionTestUtils.setField(author, "id", 101L);
        ReflectionTestUtils.setField(author, "createdAt", Instant.parse("2025-09-30T12:00:00Z"));
        ReflectionTestUtils.setField(author, "updatedAt", Instant.parse("2025-09-30T12:00:00Z"));

        Board board = Board.builder()
            .id(21L)
            .name("자유 토론")
            .description("자유롭게 의견을 나누는 공간")
            .type(BoardType.GENERAL)
            .slug("free")
            .build();
        ReflectionTestUtils.setField(board, "createdAt", Instant.parse("2025-09-01T00:00:00Z"));
        ReflectionTestUtils.setField(board, "updatedAt", Instant.parse("2025-09-01T00:00:00Z"));

        Post post = Post.builder()
            .author(author)
            .board(board)
            .title("비트코인 ETF 승인 임박")
            .content("ETF 승인 루머가 다시 돌고 있습니다.")
            .viewCount(1234L)
            .build();
        ReflectionTestUtils.setField(post, "id", 301L);
        ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2025-10-06T09:30:00Z"));
        ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2025-10-06T09:30:00Z"));

        Comment comment = Comment.builder()
            .author(author)
            .post(post)
            .content("저도 오늘 추가 매수했습니다!")
            .build();
        ReflectionTestUtils.setField(comment, "id", 401L);
        ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2025-10-06T11:40:00Z"));
        ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2025-10-06T11:40:00Z"));

        when(postRepository.findTrendingSince(any(), any())).thenReturn(List.of(post));
        when(postRepository.findRecent(any())).thenReturn(List.of(post));
        when(postRepository.findByBoard_Type(eq(BoardType.GENERAL), any())).thenReturn(new PageImpl<>(List.of(post)));
        for (BoardType type : EnumSet.complementOf(EnumSet.of(BoardType.GENERAL))) {
            when(postRepository.findByBoard_Type(eq(type), any())).thenReturn(Page.empty());
        }
        when(commentRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of(comment)));
        when(commentRepository.countByPostId(post.getId())).thenReturn(5L);
        when(voteRepository.sumVoteScore(post.getId(), VoteTargetType.POST)).thenReturn(12L);

        HomeFeedDto feed = homeFeedService.loadHomeFeed();

        assertThat(feed.trending()).hasSize(1);
        assertThat(feed.trending().get(0).viewCount()).isEqualTo(1234L);
        assertThat(feed.trending().get(0).commentCount()).isEqualTo(5L);
        assertThat(feed.trending().get(0).voteScore()).isEqualTo(12L);

        assertThat(feed.boardFeeds()).containsKey(BoardType.GENERAL);
        assertThat(feed.boardFeeds().get(BoardType.GENERAL)).hasSize(1);

        assertThat(feed.fresh()).extracting(card -> card.title()).contains("비트코인 ETF 승인 임박");

        assertThat(feed.recentComments()).hasSize(1);
        assertThat(feed.recentComments().get(0).contentSnippet()).contains("추가 매수");
    }
}
