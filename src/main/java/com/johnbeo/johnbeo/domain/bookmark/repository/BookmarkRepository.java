package com.johnbeo.johnbeo.domain.bookmark.repository;

import com.johnbeo.johnbeo.domain.bookmark.entity.Bookmark;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByMemberIdAndPostId(Long memberId, Long postId);

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    void deleteByMemberIdAndPostId(Long memberId, Long postId);

    long countByPostId(Long postId);
}
