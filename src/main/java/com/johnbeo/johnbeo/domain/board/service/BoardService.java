package com.johnbeo.johnbeo.domain.board.service;

import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.dto.CreateBoardRequest;
import com.johnbeo.johnbeo.domain.board.entity.Board;
import com.johnbeo.johnbeo.domain.board.repository.BoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    public List<BoardResponse> getBoards() {
        return boardRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
            .map(this::toBoardResponse)
            .toList();
    }

    public BoardResponse getBoard(String slug) {
        Board board = boardRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("게시판을 찾을 수 없습니다: " + slug));
        return toBoardResponse(board);
    }

    @Transactional
    public BoardResponse createBoard(CreateBoardRequest request) {
        validateBoardUniqueness(request);
        Board board = Board.builder()
            .name(request.name())
            .description(request.description())
            .slug(request.slug())
            .type(request.type())
            .build();
        Board saved = boardRepository.save(board);
        return toBoardResponse(saved);
    }

    private void validateBoardUniqueness(CreateBoardRequest request) {
        if (boardRepository.existsBySlug(request.slug())) {
            throw new ResourceAlreadyExistsException("이미 존재하는 슬러그입니다: " + request.slug());
        }
        if (boardRepository.existsByName(request.name())) {
            throw new ResourceAlreadyExistsException("이미 존재하는 게시판 이름입니다: " + request.name());
        }
    }

    private BoardResponse toBoardResponse(Board board) {
        return new BoardResponse(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getSlug(),
            board.getType(),
            board.getCreatedAt()
        );
    }
}
