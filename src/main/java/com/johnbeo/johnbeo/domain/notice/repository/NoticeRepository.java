package com.johnbeo.johnbeo.domain.notice.repository;

import com.johnbeo.johnbeo.domain.notice.entity.Notice;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByPublishedAtLessThanEqualOrderByPriorityDescPublishedAtDesc(Instant referenceTime);
}
