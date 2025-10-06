package com.johnbeo.johnbeo.domain.notice.service;

import com.johnbeo.johnbeo.domain.notice.dto.NoticeResponse;
import com.johnbeo.johnbeo.domain.notice.entity.Notice;
import com.johnbeo.johnbeo.domain.notice.repository.NoticeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    public List<NoticeResponse> getActiveNotices(int limit) {
        Instant now = Instant.now(clock);
        return noticeRepository.findByPublishedAtLessThanEqualOrderByPriorityDescPublishedAtDesc(now).stream()
            .filter(notice -> notice.isActive(now))
            .limit(limit)
            .map(this::toResponse)
            .toList();
    }

    private NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(
            notice.getId(),
            notice.getTitle(),
            notice.getContent(),
            notice.getPriority(),
            notice.getPublishedAt(),
            notice.getTargetUrl()
        );
    }
}
