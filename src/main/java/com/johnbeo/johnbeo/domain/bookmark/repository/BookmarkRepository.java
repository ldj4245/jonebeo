package com.johnbeo.johnbeo.domain.bookmark.repository;

import com.johnbeo.johnbeo.domain.bookmark.entity.Bookmark;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.post.entity.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByMemberAndPost(Member member, Post post);

    boolean existsByMemberAndPost(Member member, Post post);
}
