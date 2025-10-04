package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.service.BoardService;
import com.johnbeo.johnbeo.domain.comment.dto.CommentResponse;
import com.johnbeo.johnbeo.domain.comment.service.CommentService;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.dto.PostSummaryResponse;
import com.johnbeo.johnbeo.domain.post.service.PostService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CommunityViewController {

    private static final int MARKET_PREVIEW_SIZE = 6;

    private final BoardService boardService;
    private final PostService postService;
    private final CommentService commentService;
    private final CryptoDataService cryptoDataService;
    private final CoinGeckoProperties coinGeckoProperties;

    @GetMapping("/")
    public String home(Model model) {
        List<CoinMarketDto> markets = fetchMarketSnapshot();
        List<BoardResponse> boards = boardService.getBoards();
        model.addAttribute("pageTitle", "존비오 코인 커뮤니티");
        model.addAttribute("coins", markets);
        model.addAttribute("boards", boards);
        return "index";
    }

    @GetMapping("/boards/{slug}")
    public String board(
        @PathVariable String slug,
        @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal MemberPrincipal principal,
        Model model
    ) {
        BoardResponse board = boardService.getBoard(slug);
        PageResponse<PostSummaryResponse> posts = postService.getPostsByBoardSlug(slug, pageable);
        model.addAttribute("pageTitle", board.name());
        model.addAttribute("board", board);
        model.addAttribute("posts", posts);
        model.addAttribute("isAuthenticated", principal != null);
        return "board/detail";
    }

    @GetMapping("/posts/{id}")
    public String post(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberPrincipal principal,
        Model model
    ) {
        PostResponse post = postService.readPost(id);
        List<CommentResponse> comments = commentService.getCommentsByPost(id);
        model.addAttribute("pageTitle", post.title());
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("isAuthenticated", principal != null);
        model.addAttribute("currentUserId", principal != null ? principal.getId() : null);
        return "post/detail";
    }

    private List<CoinMarketDto> fetchMarketSnapshot() {
        try {
            String currency = coinGeckoProperties.getMarket().getVsCurrency();
            return cryptoDataService.getMarketCoins(MARKET_PREVIEW_SIZE, 1, currency);
        } catch (Exception ex) {
            log.warn("Failed to fetch market snapshot", ex);
            return Collections.emptyList();
        }
    }
}
