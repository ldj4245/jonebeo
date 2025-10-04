package com.johnbeo.johnbeo.cryptodata.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartPoint;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class CryptoDataServiceTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private CryptoDataService cryptoDataService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("coingecko.base-url", () -> mockWebServer.url("/").toString());
    }

    @Test
    void getMarketCoins_returnsCoins() throws InterruptedException {
        String body = "[\n" +
            "  {\n" +
            "    \"id\": \"bitcoin\",\n" +
            "    \"symbol\": \"btc\",\n" +
            "    \"name\": \"Bitcoin\",\n" +
            "    \"image\": \"https://assets.coingecko.com/coins/images/1/large/bitcoin.png\",\n" +
            "    \"current_price\": 50000.0,\n" +
            "    \"market_cap\": 900000000000.0,\n" +
            "    \"total_volume\": 35000000000.0,\n" +
            "    \"price_change_percentage_24h\": 1.5\n" +
            "  }\n" +
            "]";
        mockWebServer.enqueue(new MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json"));

        List<CoinMarketDto> result = cryptoDataService.getMarketCoins(10, 1, "usd");

        assertThat(result).hasSize(1);
    CoinMarketDto coin = result.get(0);
        assertThat(coin.id()).isEqualTo("bitcoin");
        assertThat(coin.currentPrice()).isEqualTo(BigDecimal.valueOf(50000.0));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/coins/markets");
        assertThat(request.getRequestUrl()).isNotNull();
        assertThat(request.getRequestUrl().queryParameter("vs_currency")).isEqualTo("usd");
    }

    @Test
    void getMarketChart_returnsPoints() {
        String body = "{\n" +
            "  \"prices\": [[1700000000000, 40000.0]],\n" +
            "  \"market_caps\": [[1700000000000, 700000000000.0]],\n" +
            "  \"total_volumes\": [[1700000000000, 25000000000.0]]\n" +
            "}";
        mockWebServer.enqueue(new MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json"));

        MarketChartDto chart = cryptoDataService.getMarketChart("bitcoin", 30, "usd");

        assertThat(chart.prices()).hasSize(1);
    MarketChartPoint point = chart.prices().get(0);
        assertThat(point.timestamp()).isEqualTo(1700000000000L);
        assertThat(point.value()).isEqualByComparingTo(BigDecimal.valueOf(40000.0));
    }
}
