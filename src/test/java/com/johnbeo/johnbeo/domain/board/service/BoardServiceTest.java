package com.johnbeo.johnbeo.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.dto.CreateBoardRequest;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(BoardService.class)
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void createBoard_successfullyPersistsEntity() {
        CreateBoardRequest request = new CreateBoardRequest("자유 게시판", "코인 이야기", "free", BoardType.GENERAL);

        BoardResponse response = boardService.createBoard(request);

        assertThat(response.id()).isNotNull();
        assertThat(boardRepository.existsBySlug("free")).isTrue();
    }

    @Test
    void createBoard_duplicateSlugThrowsException() {
        CreateBoardRequest request = new CreateBoardRequest("뉴스", "최신 소식", "news", BoardType.NEWS);
        boardService.createBoard(request);

        assertThatThrownBy(() -> boardService.createBoard(request))
            .isInstanceOf(ResourceAlreadyExistsException.class);
    }
}
