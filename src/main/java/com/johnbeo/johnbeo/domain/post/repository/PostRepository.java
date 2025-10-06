package com.johnbeo.johnbeo.domain.post.repository;

import com.johnbeo.johnbeo.domain.post.entity.Post;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "board"})
    Optional<Post> findById(Long id);

    @EntityGraph(attributePaths = {"author", "board"})
    Page<Post> findByBoardId(Long boardId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "board"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "board"})
    Page<Post> findByBoard_Type(com.johnbeo.johnbeo.domain.board.model.BoardType type, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "board"})
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.viewCount DESC")
    List<Post> findTrendingSince(Instant since, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "board"})
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findRecent(Pageable pageable);

    long countByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"board"})
    List<Post> findTop5ByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
