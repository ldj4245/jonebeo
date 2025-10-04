package com.johnbeo.johnbeo.cryptodata.dto;

import java.math.BigDecimal;

public record SimplePriceDto(BigDecimal price, BigDecimal change24h) {
}
