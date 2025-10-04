package com.johnbeo.johnbeo.domain.board.repository;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
