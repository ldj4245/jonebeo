package com.johnbeo.johnbeo.domain.board.config;

import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class BoardDataInitializer implements ApplicationRunner {

    private final BoardRepository boardRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Stream.of(
            new DefaultBoard("자유 게시판", "free", "일반 커뮤니티 대화를 위한 공간", BoardType.GENERAL),
            new DefaultBoard("뉴스", "news", "최신 코인 뉴스와 이슈 공유", BoardType.NEWS),
            new DefaultBoard("분석", "analysis", "차트와 온체인 데이터 분석", BoardType.ANALYSIS)
        ).forEach(this::createBoardIfMissing);
    }

    private void createBoardIfMissing(DefaultBoard defaultBoard) {
        if (boardRepository.existsBySlug(defaultBoard.slug())) {
            return;
        }
        Board board = Board.builder()
            .name(defaultBoard.name())
            .description(defaultBoard.description())
            .slug(defaultBoard.slug())
            .type(defaultBoard.type())
            .build();
        boardRepository.save(board);
    }

    private record DefaultBoard(String name, String slug, String description, BoardType type) {
    }
}
