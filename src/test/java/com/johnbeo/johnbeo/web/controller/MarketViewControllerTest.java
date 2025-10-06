package com.johnbeo.johnbeo.web.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.dto.CoinDetailDto;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartDto;
import com.johnbeo.johnbeo.cryptodata.dto.MarketChartPoint;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.security.jwt.JwtTokenProvider;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketViewController.class)
class MarketViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoDataService cryptoDataService;

    @MockBean
    private CoinGeckoProperties coinGeckoProperties;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private CoinGeckoProperties.Market marketConfig;

    @BeforeEach
    void setUp() {
        marketConfig = new CoinGeckoProperties.Market();
        marketConfig.setPerPage(20);
        marketConfig.setVsCurrency("usd");
        when(coinGeckoProperties.getMarket()).thenReturn(marketConfig);

        Member member = Member.builder()
            .id(1L)
            .username("tester")
            .password("password")
            .email("tester@example.com")
            .nickname("테스터")
            .role(Role.USER)
            .build();
        MemberPrincipal principal = MemberPrincipal.from(member);
        var authentication = new TestingAuthenticationToken(principal, principal.getPassword(), "ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void market_returnsViewWithCoins() throws Exception {
        List<CoinMarketDto> coins = List.of(new CoinMarketDto(
            "bitcoin",
            "btc",
            "Bitcoin",
            "https://example.com/btc.png",
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(900_000_000_000L),
            BigDecimal.valueOf(30_000_000_000L),
            BigDecimal.valueOf(1.5)
        ));
        when(cryptoDataService.getMarketCoins(anyInt(), anyInt(), Mockito.anyString())).thenReturn(coins);

        mockMvc.perform(get("/market"))
            .andExpect(status().isOk())
            .andExpect(view().name("market/list"))
            .andExpect(model().attributeExists("coins"))
            .andExpect(model().attribute("page", 1))
            .andExpect(model().attribute("size", marketConfig.getPerPage()));
    }

    @Test
    void coinDetail_populatesModel() throws Exception {
        CoinDetailDto detail = new CoinDetailDto(
            "bitcoin",
            "btc",
            "Bitcoin",
            "디지털 금",
            "https://bitcoin.org",
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(900_000_000_000L),
            BigDecimal.valueOf(1.5),
            BigDecimal.valueOf(52000),
            BigDecimal.valueOf(48000)
        );
        MarketChartDto chart = new MarketChartDto(
            List.of(new MarketChartPoint(1700000000000L, BigDecimal.valueOf(50000)) ),
            List.of(),
            List.of()
        );
        when(cryptoDataService.getCoinDetail(Mockito.eq("bitcoin"), Mockito.anyString())).thenReturn(detail);
        when(cryptoDataService.getMarketChart(Mockito.eq("bitcoin"), Mockito.anyInt(), Mockito.anyString())).thenReturn(chart);

        mockMvc.perform(get("/coins/bitcoin"))
            .andExpect(status().isOk())
            .andExpect(view().name("coins/detail"))
            .andExpect(model().attribute("coin", detail))
            .andExpect(model().attribute("vsCurrency", "USD"))
            .andExpect(model().attributeExists("chartData"));
    }
}
