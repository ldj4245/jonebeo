package com.johnbeo.johnbeo.cryptodata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketChartResponse(
    List<List<BigDecimal>> prices,
    @JsonProperty("market_caps") List<List<BigDecimal>> marketCaps,
    @JsonProperty("total_volumes") List<List<BigDecimal>> totalVolumes
) {
}
