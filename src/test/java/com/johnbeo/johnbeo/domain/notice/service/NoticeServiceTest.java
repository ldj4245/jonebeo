package com.johnbeo.johnbeo.domain.notice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnbeo.johnbeo.domain.notice.dto.NoticeResponse;
import com.johnbeo.johnbeo.domain.notice.entity.Notice;
import com.johnbeo.johnbeo.domain.notice.repository.NoticeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class NoticeServiceTest {

    @Autowired
    private NoticeRepository noticeRepository;

    private NoticeService noticeService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        Instant fixed = Instant.parse("2025-10-06T12:00:00Z");
        clock = Clock.fixed(fixed, ZoneOffset.UTC);
        noticeService = new NoticeService(noticeRepository, clock);
    }

    @Test
    void getActiveNoticesReturnsSortedAndLimitedList() {
        Instant now = Instant.now(clock);
        List<Notice> notices = List.of(
            Notice.builder()
                .title("공지 A")
                .content("내용 A")
                .priority(1)
                .publishedAt(now.minusSeconds(60))
                .targetUrl(null)
                .build(),
            Notice.builder()
                .title("공지 B")
                .content("내용 B")
                .priority(3)
                .publishedAt(now.minusSeconds(120))
                .targetUrl(null)
                .build(),
            Notice.builder()
                .title("공지 C")
                .content("내용 C")
                .priority(2)
                .publishedAt(now.plusSeconds(60))
                .targetUrl(null)
                .build()
        );
        noticeRepository.saveAll(notices);

        var responses = noticeService.getActiveNotices(2);

        assertThat(responses)
            .hasSize(2)
            .extracting(NoticeResponse::title)
            .containsExactly("공지 B", "공지 A");
    }
}
