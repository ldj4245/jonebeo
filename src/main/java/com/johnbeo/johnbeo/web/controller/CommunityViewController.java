package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.model.BoardType;
import com.johnbeo.johnbeo.domain.board.service.BoardService;
import com.johnbeo.johnbeo.domain.comment.dto.CommentResponse;
import com.johnbeo.johnbeo.domain.comment.service.CommentService;
import com.johnbeo.johnbeo.domain.feed.dto.HomeFeedDto;
import com.johnbeo.johnbeo.domain.feed.service.HomeFeedService;
import com.johnbeo.johnbeo.domain.notice.service.NoticeService;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.dto.PostSummaryResponse;
import com.johnbeo.johnbeo.domain.post.service.PostService;
import com.johnbeo.johnbeo.domain.vote.dto.VoteSummaryResponse;
import com.johnbeo.johnbeo.domain.vote.service.VoteService;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistView;
import com.johnbeo.johnbeo.domain.watchlist.service.WatchlistService;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final VoteService voteService;
    private final CryptoDataService cryptoDataService;
    private final CoinGeckoProperties coinGeckoProperties;
    private final HomeFeedService homeFeedService;
    private final NoticeService noticeService;
    private final WatchlistService watchlistService;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal MemberPrincipal principal, Model model) {
        List<CoinMarketDto> markets = fetchMarketSnapshot();
        List<BoardResponse> boards = boardService.getBoards();
        HomeFeedDto homeFeed = homeFeedService.loadHomeFeed();
        WatchlistView watchlist = watchlistService.loadWatchlist(principal);
        model.addAttribute("pageTitle", "존비오 코인 커뮤니티");
        model.addAttribute("coins", markets);
        model.addAttribute("boards", boards);
        model.addAttribute("watchlist", watchlist);
        model.addAttribute("boardTypeLabels", boardTypeLabels());
        model.addAttribute("boardEntrypoints", buildBoardEntrypoints(boards));
        model.addAttribute("notices", noticeService.getActiveNotices(5));
        model.addAttribute("homeFeed", homeFeed);
        model.addAttribute("trendingPosts", homeFeed.trending());
        model.addAttribute("freshPosts", homeFeed.fresh());
        model.addAttribute("boardFeeds", homeFeed.boardFeeds());
        model.addAttribute("recentComments", homeFeed.recentComments());
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
    PostResponse post = postService.readPost(id, principal);
        List<CommentResponse> comments = commentService.getCommentsByPost(id);
        VoteSummaryResponse postVotes = voteService.getPostVotes(id, principal);
        Map<Long, VoteSummaryResponse> commentVotes = loadCommentVotes(comments, principal);
        model.addAttribute("pageTitle", post.title());
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("postVotes", postVotes);
        model.addAttribute("commentVotes", commentVotes);
        model.addAttribute("isAuthenticated", principal != null);
        model.addAttribute("currentUserId", principal != null ? principal.getId() : null);
        return "post/detail";
    }

    private Map<Long, VoteSummaryResponse> loadCommentVotes(List<CommentResponse> comments, MemberPrincipal principal) {
        Map<Long, VoteSummaryResponse> votes = new HashMap<>();
        if (comments == null) {
            return votes;
        }
        for (CommentResponse comment : comments) {
            VoteSummaryResponse summary = voteService.getCommentVotes(comment.id(), principal);
            votes.put(comment.id(), summary);
            if (comment.replies() != null && !comment.replies().isEmpty()) {
                votes.putAll(loadCommentVotes(comment.replies(), principal));
            }
        }
        return votes;
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

    private Map<BoardType, String> boardTypeLabels() {
        Map<BoardType, String> labels = new EnumMap<>(BoardType.class);
        labels.put(BoardType.GENERAL, "자유 토론");
        labels.put(BoardType.NEWS, "속보 & 뉴스");
        labels.put(BoardType.ANALYSIS, "분석 리포트");
        labels.put(BoardType.AIRDROP, "에어드롭 / 이벤트");
        labels.put(BoardType.CALENDAR, "상장 일정");
        return labels;
    }

    private Map<BoardType, BoardResponse> buildBoardEntrypoints(List<BoardResponse> boards) {
        Map<BoardType, BoardResponse> entrypoints = new EnumMap<>(BoardType.class);
        if (boards == null) {
            return entrypoints;
        }
        for (BoardResponse board : boards) {
            if (board != null && board.type() != null) {
                entrypoints.putIfAbsent(board.type(), board);
            }
        }
        return entrypoints;
    }
}
