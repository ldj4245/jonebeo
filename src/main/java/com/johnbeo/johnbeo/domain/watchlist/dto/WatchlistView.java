package com.johnbeo.johnbeo.domain.watchlist.dto;

import java.util.List;

public record WatchlistView(
    List<WatchlistItemResponse> items,
    boolean usingDefault
) {
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
