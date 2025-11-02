package com.johnbeo.johnbeo.domain.tag.repository;

import com.johnbeo.johnbeo.domain.tag.entity.PostTag;
import com.johnbeo.johnbeo.domain.tag.entity.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    List<PostTag> findByPostId(Long postId);

    List<PostTag> findByTagId(Long tagId);

    @Query("SELECT pt.tag FROM PostTag pt WHERE pt.post.id = :postId")
    List<Tag> findTagsByPostId(Long postId);

    @Query("SELECT pt FROM PostTag pt WHERE pt.post.id = :postId AND pt.tag.id IN :tagIds")
    List<PostTag> findByPostIdAndTagIdIn(Long postId, List<Long> tagIds);

    void deleteByPostId(Long postId);
}

