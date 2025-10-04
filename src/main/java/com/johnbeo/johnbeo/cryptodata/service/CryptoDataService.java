package com.johnbeo.johnbeo.cryptodata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.johnbeo.johnbeo.common.exception.ExternalApiException;
import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinDetailDto;
import com.johnbeo.johnbeo.cryptodata.dto.CoinDetailResponse;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartPoint;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartResponse;
import com.johnbeo.johnbeo.cryptodata.dto.SimplePriceDto;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class CryptoDataService {

    private static final String CACHE_MARKET = "coins.market";
    private static final String CACHE_DETAIL = "coins.detail";
    private static final String CACHE_MARKET_CHART = "coins.marketChart";
    private static final String CACHE_SIMPLE_PRICE = "coins.simplePrice";

    private final WebClient coinGeckoWebClient;
    private final CoinGeckoProperties properties;

    @Cacheable(value = CACHE_MARKET, key = "#vsCurrency + ':' + #perPage + ':' + #page")
    public List<CoinMarketDto> getMarketCoins(int perPage, int page, String vsCurrency) {
        String normalizedCurrency = normalizeCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        try {
            return Objects.requireNonNull(coinGeckoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/coins/markets")
                    .queryParam("vs_currency", normalizedCurrency)
                    .queryParam("order", "market_cap_desc")
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .queryParam("sparkline", false)
                    .queryParam("price_change_percentage", "24h")
                    .build())
                .retrieve()
                .bodyToFlux(CoinMarketDto.class)
                .collectList()
                .block());
        } catch (WebClientResponseException ex) {
            throw toExternalApiException("CoinGecko market data", ex);
        } catch (Exception ex) {
            throw new ExternalApiException("Failed to fetch CoinGecko market data", ex);
        }
    }

    @Cacheable(value = CACHE_DETAIL, key = "#coinId + ':' + #vsCurrency")
    public CoinDetailDto getCoinDetail(String coinId, String vsCurrency) {
        String normalizedCurrency = normalizeCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        try {
            CoinDetailResponse response = coinGeckoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/coins/{id}")
                    .queryParam("localization", false)
                    .queryParam("tickers", false)
                    .queryParam("market_data", true)
                    .queryParam("community_data", false)
                    .queryParam("developer_data", false)
                    .queryParam("sparkline", false)
                    .build(coinId))
                .retrieve()
                .bodyToMono(CoinDetailResponse.class)
                .block();

            if (response == null) {
                throw new ExternalApiException("CoinGecko returned empty response for coin detail");
            }

            return toCoinDetailDto(response, normalizedCurrency);
        } catch (WebClientResponseException ex) {
            throw toExternalApiException("CoinGecko coin detail", ex);
        } catch (Exception ex) {
            throw new ExternalApiException("Failed to fetch CoinGecko coin detail", ex);
        }
    }

    @Cacheable(value = CACHE_MARKET_CHART, key = "#coinId + ':' + #days + ':' + #vsCurrency")
    public MarketChartDto getMarketChart(String coinId, int days, String vsCurrency) {
        String normalizedCurrency = normalizeCurrency(vsCurrency, properties.getMarket().getVsCurrency());
        try {
            MarketChartResponse response = coinGeckoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/coins/{id}/market_chart")
                    .queryParam("vs_currency", normalizedCurrency)
                    .queryParam("days", days)
                    .queryParam("interval", days >= 90 ? "daily" : "hourly")
                    .build(coinId))
                .retrieve()
                .bodyToMono(MarketChartResponse.class)
                .block();

            if (response == null) {
                throw new ExternalApiException("CoinGecko returned empty response for market chart");
            }

            return new MarketChartDto(
                mapToPoints(response.prices()),
                mapToPoints(response.marketCaps()),
                mapToPoints(response.totalVolumes())
            );
        } catch (WebClientResponseException ex) {
            throw toExternalApiException("CoinGecko market chart", ex);
        } catch (Exception ex) {
            throw new ExternalApiException("Failed to fetch CoinGecko market chart", ex);
        }
    }

    @Cacheable(value = CACHE_SIMPLE_PRICE, key = "#coinIds + ':' + #vsCurrency")
    public Map<String, SimplePriceDto> getSimplePrices(List<String> coinIds, String vsCurrency) {
        if (coinIds == null || coinIds.isEmpty()) {
            throw new IllegalArgumentException("coinIds must not be empty");
        }
        String normalizedCurrency = normalizeCurrency(vsCurrency, properties.getSimplePrice().getVsCurrency());
        try {
            JsonNode root = coinGeckoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/simple/price")
                    .queryParam("ids", String.join(",", coinIds))
                    .queryParam("vs_currencies", normalizedCurrency)
                    .queryParam("include_24hr_change", properties.getSimplePrice().isInclude24hChange())
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (root == null) {
                throw new ExternalApiException("CoinGecko returned empty response for simple price");
            }

            Map<String, SimplePriceDto> result = new LinkedHashMap<>();
            for (String coinId : coinIds) {
                JsonNode coinNode = root.get(coinId);
                if (coinNode == null || coinNode.isNull()) {
                    continue;
                }
                JsonNode priceNode = coinNode.get(normalizedCurrency);
                if (priceNode == null || priceNode.isNull()) {
                    continue;
                }
                BigDecimal price = priceNode.decimalValue();
                BigDecimal change24h = Optional.ofNullable(coinNode.get(normalizedCurrency + "_24h_change"))
                    .map(JsonNode::decimalValue)
                    .orElse(null);
                result.put(coinId, new SimplePriceDto(price, change24h));
            }
            return result;
        } catch (WebClientResponseException ex) {
            throw toExternalApiException("CoinGecko simple price", ex);
        } catch (Exception ex) {
            throw new ExternalApiException("Failed to fetch CoinGecko simple price", ex);
        }
    }

    private CoinDetailDto toCoinDetailDto(CoinDetailResponse response, String currency) {
        CoinDetailResponse.MarketData marketData = response.marketData();
        String description = Optional.ofNullable(response.description())
            .map(desc -> desc.getOrDefault("ko", desc.getOrDefault("en", "")))
            .orElse("");
        String homepage = Optional.ofNullable(response.links())
            .map(CoinDetailResponse.CoinLinks::homepage)
            .orElse(List.of())
            .stream()
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse("");
        Map<String, BigDecimal> currentPriceMap = marketData != null ? marketData.currentPrice() : Map.of();
        Map<String, BigDecimal> marketCapMap = marketData != null ? marketData.marketCap() : Map.of();
        Map<String, BigDecimal> high24hMap = marketData != null ? marketData.high24h() : Map.of();
        Map<String, BigDecimal> low24hMap = marketData != null ? marketData.low24h() : Map.of();
        BigDecimal priceChange = marketData != null ? marketData.priceChangePercentage24h() : null;
        return new CoinDetailDto(
            response.id(),
            response.symbol(),
            response.name(),
            description,
            homepage,
            currentPriceMap.get(currency),
            marketCapMap.get(currency),
            priceChange,
            high24hMap.get(currency),
            low24hMap.get(currency)
        );
    }

    private List<MarketChartPoint> mapToPoints(List<List<BigDecimal>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(list -> list.size() >= 2)
            .map(list -> new MarketChartPoint(list.get(0).longValue(), list.get(1)))
            .collect(Collectors.toList());
    }

    private String normalizeCurrency(String currency, String defaultCurrency) {
        String value = StringUtils.hasText(currency) ? currency : defaultCurrency;
        return value == null ? "usd" : value.toLowerCase(Locale.ROOT);
    }

    private ExternalApiException toExternalApiException(String label, WebClientResponseException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        String message = String.format("%s error (status: %d): %s", label, statusCode.value(), ex.getResponseBodyAsString());
        return new ExternalApiException(message, ex);
    }
}
