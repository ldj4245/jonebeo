package com.johnbeo.johnbeo.domain.comment.service;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.comment.dto.CommentAuthorDto;
import com.johnbeo.johnbeo.domain.comment.dto.CommentResponse;
import com.johnbeo.johnbeo.domain.comment.dto.CreateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.dto.UpdateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import com.johnbeo.johnbeo.domain.comment.repository.CommentRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import com.johnbeo.johnbeo.domain.post.repository.PostRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public List<CommentResponse> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Long, CommentResponseBuilder> responseMap = new HashMap<>();
        List<CommentResponseBuilder> roots = new ArrayList<>();

        for (Comment comment : comments) {
            CommentResponseBuilder builder = toBuilder(comment);
            responseMap.put(comment.getId(), builder);
            if (comment.getParent() != null) {
                CommentResponseBuilder parentBuilder = responseMap.get(comment.getParent().getId());
                if (parentBuilder != null) {
                    parentBuilder.replies.add(builder);
                }
            } else {
                roots.add(builder);
            }
        }

        return roots.stream()
            .map(CommentResponseBuilder::build)
            .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, MemberPrincipal principal) {
        Member author = requirePrincipal(principal);
        Post post = postRepository.findById(request.postId())
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + request.postId()));

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + request.parentId()));
            if (!Objects.equals(parent.getPost().getId(), post.getId())) {
                throw new IllegalArgumentException("부모 댓글이 동일한 게시글에 속하지 않습니다.");
            }
        }

        Comment comment = Comment.builder()
            .author(author)
            .post(post)
            .content(request.content())
            .parent(parent)
            .build();

        Comment saved = commentRepository.save(comment);
        return toResponse(saved, List.of());
    }

    @Transactional
    public CommentResponse updateComment(Long id, UpdateCommentRequest request, MemberPrincipal principal) {
        Comment comment = findComment(id);
        Member member = requirePrincipal(principal);
        validateOwnership(comment, member);
        comment.updateContent(request.content());
        return toResponse(comment, comment.getChildren().stream().map(child -> toResponse(child, List.of())).toList());
    }

    @Transactional
    public void deleteComment(Long id, MemberPrincipal principal) {
        Comment comment = findComment(id);
        Member member = requirePrincipal(principal);
        validateOwnership(comment, member);
        commentRepository.delete(comment);
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + id));
    }

    private Member requirePrincipal(MemberPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return memberRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("회원 정보를 찾을 수 없습니다: " + principal.getId()));
    }

    private void validateOwnership(Comment comment, Member member) {
        if (!Objects.equals(comment.getAuthor().getId(), member.getId())) {
            throw new AccessDeniedException("댓글에 대한 권한이 없습니다.");
        }
    }

    private CommentResponseBuilder toBuilder(Comment comment) {
        CommentResponseBuilder builder = new CommentResponseBuilder();
        builder.id = comment.getId();
        builder.content = comment.getContent();
        builder.createdAt = comment.getCreatedAt();
        builder.updatedAt = comment.getUpdatedAt();
        builder.author = new CommentAuthorDto(comment.getAuthor().getId(), comment.getAuthor().getNickname());
        builder.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        return builder;
    }

    private CommentResponse toResponse(Comment comment, List<CommentResponse> replies) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            new CommentAuthorDto(comment.getAuthor().getId(), comment.getAuthor().getNickname()),
            comment.getParent() != null ? comment.getParent().getId() : null,
            replies
        );
    }

    private static class CommentResponseBuilder {
        private Long id;
        private String content;
        private java.time.Instant createdAt;
        private java.time.Instant updatedAt;
        private CommentAuthorDto author;
        private Long parentId;
        private final List<CommentResponseBuilder> replies = new ArrayList<>();

        private CommentResponse build() {
            return new CommentResponse(
                id,
                content,
                createdAt,
                updatedAt,
                author,
                parentId,
                replies.stream().map(CommentResponseBuilder::build).toList()
            );
        }
    }
}
