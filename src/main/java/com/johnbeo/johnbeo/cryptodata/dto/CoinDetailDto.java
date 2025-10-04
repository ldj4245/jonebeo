package com.johnbeo.johnbeo.cryptodata.dto;

import java.math.BigDecimal;

public record CoinDetailDto(
    String id,
    String symbol,
    String name,
    String description,
    String homepage,
    BigDecimal currentPrice,
    BigDecimal marketCap,
    BigDecimal priceChangePercentage24h,
    BigDecimal high24h,
    BigDecimal low24h
) {
}
