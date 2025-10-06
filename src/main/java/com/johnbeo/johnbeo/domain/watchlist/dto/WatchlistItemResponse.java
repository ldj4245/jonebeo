package com.johnbeo.johnbeo.domain.watchlist.dto;

import java.math.BigDecimal;
import java.util.Optional;

public record WatchlistItemResponse(
    String coinId,
    String symbol,
    String name,
    String label,
    String image,
    BigDecimal priceKrw,
    BigDecimal priceUsd,
    BigDecimal change24h,
    BigDecimal premiumRate,
    BigDecimal volumeUsd,
    boolean custom
) {

    public Optional<BigDecimal> priceKrwOptional() {
        return Optional.ofNullable(priceKrw);
    }

    public Optional<BigDecimal> priceUsdOptional() {
        return Optional.ofNullable(priceUsd);
    }

    public Optional<BigDecimal> change24hOptional() {
        return Optional.ofNullable(change24h);
    }

    public Optional<BigDecimal> premiumRateOptional() {
        return Optional.ofNullable(premiumRate);
    }

    public Optional<BigDecimal> volumeUsdOptional() {
        return Optional.ofNullable(volumeUsd);
    }
}
