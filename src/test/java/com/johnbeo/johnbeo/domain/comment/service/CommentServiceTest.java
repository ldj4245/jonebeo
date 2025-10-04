package com.johnbeo.johnbeo.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import com.johnbeo.johnbeo.domain.comment.dto.CommentResponse;
import com.johnbeo.johnbeo.domain.comment.dto.CreateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CommentService.class)
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private MemberPrincipal authorPrincipal;
    private Post post;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
            .username("member1")
            .password("encoded")
            .email("member1@example.com")
            .nickname("멤버1")
            .role(Role.USER)
            .build());
        authorPrincipal = MemberPrincipal.from(member);
        Board board = boardRepository.save(Board.builder()
            .name("테스트 게시판")
            .description("설명")
            .slug("test-board")
            .type(BoardType.GENERAL)
            .build());
        post = postRepository.save(Post.builder()
            .author(member)
            .board(board)
            .title("테스트 게시글")
            .content("본문")
            .build());
    }

    @Test
    void createAndListComments_withHierarchy() {
        CommentResponse parent = commentService.createComment(new CreateCommentRequest(post.getId(), null, "부모 댓글"), authorPrincipal);
        commentService.createComment(new CreateCommentRequest(post.getId(), parent.id(), "대댓글"), authorPrincipal);

        List<CommentResponse> comments = commentService.getCommentsByPost(post.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).content()).isEqualTo("부모 댓글");
        assertThat(comments.get(0).replies()).hasSize(1);
        assertThat(comments.get(0).replies().get(0).content()).isEqualTo("대댓글");
    }
}
