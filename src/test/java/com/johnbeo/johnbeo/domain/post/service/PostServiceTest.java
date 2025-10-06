package com.johnbeo.johnbeo.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import com.johnbeo.johnbeo.domain.bookmark.service.BookmarkService;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.dto.CreatePostRequest;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.post.service.support.PostViewTracker;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DataJpaTest
@Import({PostService.class, BookmarkService.class, PostViewTracker.class, PostServiceTest.TestCacheConfig.class})
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private MemberPrincipal authorPrincipal;
    private MemberPrincipal anotherPrincipal;
    private Board board;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(Member.builder()
            .username("writer1")
            .password("encoded-password")
            .email("writer1@example.com")
            .nickname("작가1")
            .role(Role.USER)
            .build());
        authorPrincipal = MemberPrincipal.from(author);

        Member another = memberRepository.save(Member.builder()
            .username("reader2")
            .password("encoded-password")
            .email("reader2@example.com")
            .nickname("독자2")
            .role(Role.USER)
            .build());
        anotherPrincipal = MemberPrincipal.from(another);

        board = boardRepository.save(Board.builder()
            .name("자유 게시판")
            .description("일반 토론")
            .slug("free")
            .type(BoardType.GENERAL)
            .build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createPost_persistsAndReturnsResponse() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "첫 글", "내용");

        PostResponse response = postService.createPost(request, authorPrincipal);

        assertThat(response.id()).isNotNull();
        assertThat(postRepository.findById(response.id())).isPresent();
    assertThat(response.bookmarkCount()).isZero();
    assertThat(response.bookmarked()).isFalse();
    }

    @Test
    void readPost_sameAuthenticatedUserCountsOnceWithinWindow() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "조회수 테스트", "내용");
        PostResponse created = postService.createPost(request, authorPrincipal);

        PostResponse firstVisit = postService.readPost(created.id(), authorPrincipal);
        PostResponse secondVisit = postService.readPost(created.id(), authorPrincipal);

        assertThat(firstVisit.viewCount()).isEqualTo(1L);
        assertThat(secondVisit.viewCount()).isEqualTo(1L);
    assertThat(firstVisit.bookmarked()).isFalse();
    }

    @Test
    void readPost_differentAuthenticatedUsersIncreaseIndependently() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "조회수 테스트", "내용");
        PostResponse created = postService.createPost(request, authorPrincipal);

        PostResponse authorVisit = postService.readPost(created.id(), authorPrincipal);
        PostResponse otherVisit = postService.readPost(created.id(), anotherPrincipal);

        assertThat(authorVisit.viewCount()).isEqualTo(1L);
        assertThat(otherVisit.viewCount()).isEqualTo(2L);
    assertThat(authorVisit.bookmarked()).isFalse();
    assertThat(otherVisit.bookmarked()).isFalse();
    }

    @Test
    void readPost_sameGuestWithSameFingerprintCountsOnce() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "게스트 조회 테스트", "내용");
        PostResponse created = postService.createPost(request, authorPrincipal);

        simulateGuestRequest("203.0.113.10", "JUnit Browser");
        PostResponse firstVisit = postService.readPost(created.id(), null);

        simulateGuestRequest("203.0.113.10", "JUnit Browser");
        PostResponse secondVisit = postService.readPost(created.id(), null);

        assertThat(firstVisit.viewCount()).isEqualTo(1L);
        assertThat(secondVisit.viewCount()).isEqualTo(1L);
    }

    private void simulateGuestRequest(String ip, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", ip);
        request.addHeader("User-Agent", userAgent);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @TestConfiguration
    static class TestCacheConfig {

        @Bean
        @Qualifier("postViewCache")
        Cache<String, Boolean> postViewCache() {
            return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(10_000)
                .build();
        }
    }
}
