package com.johnbeo.johnbeo.cryptodata.dto;

import java.util.List;

public record MarketChartDto(
    List<MarketChartPoint> prices,
    List<MarketChartPoint> marketCaps,
    List<MarketChartPoint> totalVolumes
) {
}
