package com.johnbeo.johnbeo.domain.feed.dto;

import com.johnbeo.johnbeo.domain.board.model.BoardType;
import java.util.List;
import java.util.Map;

public record HomeFeedDto(
    List<HomePostCard> trending,
    List<HomePostCard> fresh,
    Map<BoardType, List<HomePostCard>> boardFeeds,
    List<RecentCommentCard> recentComments
) {
}
