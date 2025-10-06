package com.johnbeo.johnbeo.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.config.TradingViewProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinDetailDto;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MarketViewController {

    private final CryptoDataService cryptoDataService;
    private final CoinGeckoProperties properties;
    private final TradingViewProperties tradingViewProperties;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_DAYS = 30;

    @GetMapping("/market")
    public String market(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency,
        Model model
    ) {
        int perPage = size != null ? size : properties.getMarket().getPerPage();
        String currency = resolveCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        List<CoinMarketDto> coins = cryptoDataService.getMarketCoins(perPage, page, currency);
        model.addAttribute("pageTitle", "실시간 코인 시세");
        model.addAttribute("coins", coins);
        model.addAttribute("page", page);
        model.addAttribute("size", perPage);
        model.addAttribute("vsCurrency", currency.toUpperCase());
        return "market/list";
    }

    @GetMapping("/coins/{coinId}")
    public String coinDetail(
        @PathVariable String coinId,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency,
        @RequestParam(required = false, defaultValue = "30") int days,
        Model model
    ) {
        String currency = resolveCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        int resolvedDays = days > 0 ? days : DEFAULT_DAYS;
        CoinDetailDto detail = cryptoDataService.getCoinDetail(coinId, currency);
        String tradingViewSymbol = resolveTradingViewSymbol(detail);
        MarketChartDto chart = tradingViewSymbol == null
            ? cryptoDataService.getMarketChart(coinId, resolvedDays, currency)
            : null;
        model.addAttribute("pageTitle", detail.name() + " 시세");
        model.addAttribute("coin", detail);
        model.addAttribute("vsCurrency", currency.toUpperCase());
        model.addAttribute("days", resolvedDays);
        model.addAttribute("chartData", chart != null ? toJson(chart) : "{}");
        model.addAttribute("tradingViewSymbol", tradingViewSymbol);
        return "coins/detail";
    }

    private String resolveTradingViewSymbol(CoinDetailDto detail) {
        return tradingViewProperties.resolveSymbol(detail.id(), detail.symbol())
            .orElse(null);
    }

    private String resolveCurrency(String currency, String fallback) {
        if (currency != null && !currency.isBlank()) {
            return currency.toLowerCase();
        }
        return fallback != null ? fallback.toLowerCase() : "usd";
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize chart data", ex);
            return "{}";
        }
    }
}
