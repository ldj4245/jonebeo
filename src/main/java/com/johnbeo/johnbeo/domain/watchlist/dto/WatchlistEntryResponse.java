package com.johnbeo.johnbeo.domain.watchlist.dto;

public record WatchlistEntryResponse(
    Long id,
    String coinId,
    String label,
    int displayOrder
) {
}
