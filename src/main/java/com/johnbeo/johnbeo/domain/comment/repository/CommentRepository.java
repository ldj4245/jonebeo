package com.johnbeo.johnbeo.domain.comment.repository;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "parent", "parent.author"})
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    @EntityGraph(attributePaths = {"author", "post"})
    Optional<Comment> findById(Long id);

    long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author", "post", "post.board"})
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByPostId(Long postId);
}
