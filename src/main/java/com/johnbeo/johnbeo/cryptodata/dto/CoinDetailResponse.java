package com.johnbeo.johnbeo.cryptodata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinDetailResponse(
    String id,
    String symbol,
    String name,
    Map<String, String> description,
    @JsonProperty("market_data") MarketData marketData,
    CoinLinks links
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarketData(
        @JsonProperty("current_price") Map<String, BigDecimal> currentPrice,
        @JsonProperty("market_cap") Map<String, BigDecimal> marketCap,
        @JsonProperty("high_24h") Map<String, BigDecimal> high24h,
        @JsonProperty("low_24h") Map<String, BigDecimal> low24h,
        @JsonProperty("price_change_percentage_24h") BigDecimal priceChangePercentage24h
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoinLinks(List<String> homepage) {
    }
}
