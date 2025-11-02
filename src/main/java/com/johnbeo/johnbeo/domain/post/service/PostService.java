package com.johnbeo.johnbeo.domain.post.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import com.johnbeo.johnbeo.domain.bookmark.service.BookmarkService;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.dto.CreatePostRequest;
import com.johnbeo.johnbeo.domain.post.dto.PostAuthorDto;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.dto.PostSummaryResponse;
import com.johnbeo.johnbeo.domain.post.dto.UpdatePostRequest;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.domain.post.service.support.PostViewTracker;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import com.johnbeo.johnbeo.domain.member.event.PostCreatedEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BookmarkService bookmarkService;
    private final PostViewTracker postViewTracker;
    private final ApplicationEventPublisher eventPublisher;

    public PageResponse<PostSummaryResponse> getAllPosts(Pageable pageable) {
        Page<Post> page = postRepository.findAll(pageable);
        return PageResponse.from(page.map(this::toPostSummaryResponse));
    }

    public PageResponse<PostSummaryResponse> getPostsByBoardSlug(String slug, Pageable pageable) {
        Board board = boardRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("게시판을 찾을 수 없습니다: " + slug));
        Page<Post> page = postRepository.findByBoardId(board.getId(), pageable);
        return PageResponse.from(page.map(this::toPostSummaryResponse));
    }

    public PostResponse getPost(Long id) {
    Post post = findPost(id);
    return toPostResponse(post, null);
    }

    @Transactional
    public PostResponse readPost(Long id, MemberPrincipal principal) {
        Post post = findPost(id);
        if (postViewTracker.shouldCountView(post.getId(), principal)) {
            post.incrementViewCount();
        }
        return toPostResponse(post, principal);
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request, MemberPrincipal principal) {
        Member member = requireMember(principal);
        Board board = boardRepository.findById(request.boardId())
            .orElseThrow(() -> new ResourceNotFoundException("게시판을 찾을 수 없습니다: " + request.boardId()));

        Post post = Post.builder()
            .author(member)
            .board(board)
            .title(request.title())
            .content(request.content())
            .build();

        Post saved = postRepository.save(post);
        
        // 게시글 생성 이벤트 발행
        eventPublisher.publishEvent(new PostCreatedEvent(saved));
        
        return toPostResponse(saved, principal);
    }

    @Transactional
    public PostResponse updatePost(Long id, UpdatePostRequest request, MemberPrincipal principal) {
        Post post = findPost(id);
        validateOwnership(post, principal);
    post.updateContent(request.title(), request.content());
    return toPostResponse(post, principal);
    }

    @Transactional
    public void deletePost(Long id, MemberPrincipal principal) {
        Post post = findPost(id);
        validateOwnership(post, principal);
        postRepository.delete(post);
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + id));
    }

    private Member requireMember(MemberPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return memberRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다: " + principal.getId()));
    }

    private void validateOwnership(Post post, MemberPrincipal principal) {
        Member member = requireMember(principal);
        if (!Objects.equals(post.getAuthor().getId(), member.getId())) {
            throw new AccessDeniedException("게시글에 대한 권한이 없습니다.");
        }
    }

    private PostResponse toPostResponse(Post post, MemberPrincipal principal) {
        long bookmarkCount = bookmarkService.countByPost(post.getId());
        boolean bookmarked = principal != null && bookmarkService.isBookmarked(principal.getId(), post.getId());

        var board = post.getBoard() != null ? toBoardResponse(post.getBoard()) : null;
        var author = post.getAuthor() != null
            ? toPostAuthor(post.getAuthor())
            : new PostAuthorDto(null, "알 수 없는 작성자");

        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getViewCount(),
            bookmarkCount,
            bookmarked,
            post.getCreatedAt(),
            post.getUpdatedAt(),
            board,
            author
        );
    }

    private PostSummaryResponse toPostSummaryResponse(Post post) {
        return new PostSummaryResponse(
            post.getId(),
            post.getTitle(),
            post.getViewCount(),
            post.getCreatedAt(),
            toPostAuthor(post.getAuthor())
        );
    }

    private BoardResponse toBoardResponse(Board board) {
        return new BoardResponse(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getSlug(),
            board.getType(),
            board.getCreatedAt()
        );
    }

    private PostAuthorDto toPostAuthor(Member member) {
        return new PostAuthorDto(member.getId(), member.getNickname());
    }
}
