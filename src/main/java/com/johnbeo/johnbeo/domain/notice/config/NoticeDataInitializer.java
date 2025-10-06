package com.johnbeo.johnbeo.domain.notice.config;

import com.johnbeo.johnbeo.domain.notice.entity.Notice;
import com.johnbeo.johnbeo.domain.notice.repository.NoticeRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class NoticeDataInitializer implements ApplicationRunner {

    private final NoticeRepository noticeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (noticeRepository.count() > 0) {
            return;
        }
        List<Notice> samples = List.of(
            Notice.builder()
                .title("커뮤니티 가이드 라인 안내")
                .content("존비오 커뮤니티 이용 수칙을 확인해주세요. 건전한 토론 문화를 위해 모두 함께 노력해요.")
                .priority(2)
                .publishedAt(Instant.now().minusSeconds(3600))
                .targetUrl("/boards/free")
                .build(),
            Notice.builder()
                .title("CoinGecko API 점검 일정")
                .content("10월 10일 02시~03시(UTC)에 데이터 갱신이 지연될 수 있습니다.")
                .priority(3)
                .publishedAt(Instant.now().minusSeconds(7200))
                .targetUrl(null)
                .build(),
            Notice.builder()
                .title("에어드롭 캘린더 베타 오픈")
                .content("에어드롭 소식을 한눈에 확인할 수 있는 캘린더 베타 버전을 공개했습니다.")
                .priority(1)
                .publishedAt(Instant.now().minusSeconds(10800))
                .targetUrl("/boards/calendar")
                .build()
        );
        noticeRepository.saveAll(samples);
    }
}
