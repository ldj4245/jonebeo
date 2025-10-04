package com.johnbeo.johnbeo.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.dto.CreatePostRequest;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(PostService.class)
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
    private Board board;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
            .username("writer1")
            .password("encoded-password")
            .email("writer1@example.com")
            .nickname("작가1")
            .role(Role.USER)
            .build());
        authorPrincipal = MemberPrincipal.from(member);
        board = boardRepository.save(Board.builder()
            .name("자유 게시판")
            .description("일반 토론")
            .slug("free")
            .type(BoardType.GENERAL)
            .build());
    }

    @Test
    void createPost_persistsAndReturnsResponse() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "첫 글", "내용");

        PostResponse response = postService.createPost(request, authorPrincipal);

        assertThat(response.id()).isNotNull();
        assertThat(postRepository.findById(response.id())).isPresent();
    }

    @Test
    void readPost_incrementsViewCount() {
        CreatePostRequest request = new CreatePostRequest(board.getId(), "조회수 테스트", "내용");
        PostResponse created = postService.createPost(request, authorPrincipal);

        PostResponse afterFirstRead = postService.readPost(created.id());
        PostResponse afterSecondRead = postService.readPost(created.id());

        assertThat(afterFirstRead.viewCount()).isEqualTo(1L);
        assertThat(afterSecondRead.viewCount()).isEqualTo(2L);
    }
}
