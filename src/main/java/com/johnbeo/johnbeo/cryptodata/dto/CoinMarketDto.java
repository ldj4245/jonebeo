package com.johnbeo.johnbeo.cryptodata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinMarketDto(
    String id,
    String symbol,
    String name,
    String image,
    @JsonProperty("current_price") BigDecimal currentPrice,
    @JsonProperty("market_cap") BigDecimal marketCap,
    @JsonProperty("total_volume") BigDecimal totalVolume,
    @JsonProperty("price_change_percentage_24h") BigDecimal priceChangePercentage24h
) {
}
