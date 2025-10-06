package com.johnbeo.johnbeo.domain.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import com.johnbeo.johnbeo.domain.bookmark.repository.BookmarkRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(BookmarkService.class)
class BookmarkServiceTest {

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
            .username("bookmark-user")
            .password("password")
            .email("bookmark@example.com")
            .nickname("북마커")
            .role(Role.USER)
            .build());

        Board board = boardRepository.save(Board.builder()
            .name("뉴스")
            .description("북마크 테스트 게시판")
            .slug("news")
            .type(BoardType.GENERAL)
            .build());

        post = postRepository.save(Post.builder()
            .author(member)
            .board(board)
            .title("테스트 게시글")
            .content("내용")
            .build());
    }

    @Test
    void addBookmark_persistsEntityAndUpdatesCount() {
        bookmarkService.addBookmark(member.getId(), post.getId());

        assertThat(bookmarkRepository.existsByMemberIdAndPostId(member.getId(), post.getId())).isTrue();
        assertThat(bookmarkService.isBookmarked(member.getId(), post.getId())).isTrue();
        assertThat(bookmarkService.countByPost(post.getId())).isEqualTo(1L);
    }

    @Test
    void addBookmark_isIdempotent() {
        bookmarkService.addBookmark(member.getId(), post.getId());
        bookmarkService.addBookmark(member.getId(), post.getId());

        assertThat(bookmarkService.countByPost(post.getId())).isEqualTo(1L);
    }

    @Test
    void removeBookmark_deletesEntry() {
        bookmarkService.addBookmark(member.getId(), post.getId());

        bookmarkService.removeBookmark(member.getId(), post.getId());

        assertThat(bookmarkService.isBookmarked(member.getId(), post.getId())).isFalse();
        assertThat(bookmarkService.countByPost(post.getId())).isZero();
    }

    @Test
    void removeBookmark_handlesMissingBookmarkGracefully() {
        bookmarkService.removeBookmark(member.getId(), post.getId());

        assertThat(bookmarkService.isBookmarked(member.getId(), post.getId())).isFalse();
    }
}
