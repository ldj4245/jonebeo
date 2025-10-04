package com.johnbeo.johnbeo.domain.comment.repository;

import com.johnbeo.johnbeo.domain.comment.entity.Comment;
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
}
