package com.johnbeo.johnbeo.cryptodata.dto;

import java.math.BigDecimal;

public record MarketChartPoint(long timestamp, BigDecimal value) {
}
