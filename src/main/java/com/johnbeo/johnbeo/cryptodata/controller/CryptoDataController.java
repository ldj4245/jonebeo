package com.johnbeo.johnbeo.cryptodata.controller;

import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinDetailDto;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartDto;
import com.johnbeo.johnbeo.cryptodata.dto.SimplePriceDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CryptoDataController {

    private final CryptoDataService cryptoDataService;
    private final CoinGeckoProperties properties;

    @GetMapping("/markets")
    public ResponseEntity<List<CoinMarketDto>> getMarkets(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Integer perPage,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency
    ) {
        int resolvedPerPage = perPage != null ? perPage : properties.getMarket().getPerPage();
        String resolvedCurrency = resolveCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        List<CoinMarketDto> markets = cryptoDataService.getMarketCoins(resolvedPerPage, page, resolvedCurrency);
        return ResponseEntity.ok(markets);
    }

    @GetMapping("/{coinId}")
    public ResponseEntity<CoinDetailDto> getCoinDetail(
        @PathVariable String coinId,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency
    ) {
        String resolvedCurrency = resolveCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        CoinDetailDto detail = cryptoDataService.getCoinDetail(coinId, resolvedCurrency);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/{coinId}/market-chart")
    public ResponseEntity<MarketChartDto> getMarketChart(
        @PathVariable String coinId,
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency
    ) {
        String resolvedCurrency = resolveCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        MarketChartDto chart = cryptoDataService.getMarketChart(coinId, days, resolvedCurrency);
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/simple-price")
    public ResponseEntity<Map<String, SimplePriceDto>> getSimplePrices(
        @RequestParam(name = "ids") List<String> coinIds,
        @RequestParam(required = false, name = "vs_currency") String vsCurrency
    ) {
        String resolvedCurrency = resolveCurrency(vsCurrency, properties.getSimplePrice().getVsCurrency());
        Map<String, SimplePriceDto> prices = cryptoDataService.getSimplePrices(coinIds, resolvedCurrency);
        return ResponseEntity.ok(prices);
    }

    private String resolveCurrency(String currency, String fallback) {
        if (StringUtils.hasText(currency)) {
            return currency;
        }
        return fallback;
    }
}
