package com.johnbeo.johnbeo.domain.tag.repository;

import com.johnbeo.johnbeo.domain.tag.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(List<String> names);

    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC")
    List<Tag> findPopularTags(Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE t.name LIKE %:keyword% ORDER BY t.usageCount DESC")
    List<Tag> searchByKeyword(String keyword, Pageable pageable);
}

