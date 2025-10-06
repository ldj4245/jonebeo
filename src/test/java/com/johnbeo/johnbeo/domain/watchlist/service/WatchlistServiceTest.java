package com.johnbeo.johnbeo.domain.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.watchlist.config.WatchlistProperties;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistView;
import com.johnbeo.johnbeo.domain.watchlist.entity.WatchlistEntry;
import com.johnbeo.johnbeo.domain.watchlist.repository.WatchlistEntryRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistEntryRepository watchlistEntryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CryptoDataService cryptoDataService;

    private WatchlistProperties watchlistProperties;

    @InjectMocks
    private WatchlistService watchlistService;

    @Captor
    private ArgumentCaptor<WatchlistEntry> entryCaptor;

    @BeforeEach
    void setUp() {
        watchlistProperties = new WatchlistProperties();
        watchlistProperties.setUsdToKrwRate(BigDecimal.valueOf(1350));
        watchlistProperties.setDefaults(List.of(
            new WatchlistProperties.DefaultCoin("bitcoin", "비트코인")
        ));
        watchlistService = new WatchlistService(watchlistEntryRepository, memberRepository, cryptoDataService, watchlistProperties);
    }

    @Test
    void loadWatchlistReturnsDefaultsForGuest() {
        when(cryptoDataService.getMarketCoinsByIds(eq(List.of("bitcoin")), eq("usd")))
            .thenReturn(List.of(sampleMarket("bitcoin", "btc", BigDecimal.valueOf(65000), BigDecimal.valueOf(1.5), BigDecimal.valueOf(40000000000L))));
        when(cryptoDataService.getMarketCoinsByIds(eq(List.of("bitcoin")), eq("krw")))
            .thenReturn(List.of(sampleMarket("bitcoin", "btc", BigDecimal.valueOf(85000000), BigDecimal.valueOf(1.5), BigDecimal.ZERO)));

        WatchlistView view = watchlistService.loadWatchlist(null);

        assertThat(view.usingDefault()).isTrue();
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).label()).isEqualTo("비트코인");
        assertThat(view.items().get(0).priceUsd()).isEqualTo(BigDecimal.valueOf(65000));
    }

    @Test
    void addEntryPersistsNewCoin() {
        Long memberId = 1L;
        Member member = Member.builder()
            .id(memberId)
            .username("tester")
            .password("secret")
            .email("tester@example.com")
            .nickname("테스터")
            .role(Role.USER)
            .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(watchlistEntryRepository.countByMemberId(memberId)).thenReturn(0);
        when(watchlistEntryRepository.existsByMemberIdAndCoinIdIgnoreCase(memberId, "ethereum")).thenReturn(false);
        when(cryptoDataService.getMarketCoinsByIds(eq(List.of("ethereum")), eq("usd")))
            .thenReturn(List.of(sampleMarket("ethereum", "eth", BigDecimal.valueOf(3000), BigDecimal.valueOf(2.1), BigDecimal.valueOf(20000000000L))));
        when(watchlistEntryRepository.findMaxDisplayOrder(memberId)).thenReturn(4);

        watchlistService.addEntry(memberId, "Ethereum", "이더리움");

        verify(watchlistEntryRepository).save(entryCaptor.capture());
        WatchlistEntry saved = entryCaptor.getValue();
        assertThat(saved.getCoinId()).isEqualTo("ethereum");
        assertThat(saved.getLabel()).isEqualTo("이더리움");
        assertThat(saved.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void addEntryThrowsWhenLimitExceeded() {
        Long memberId = 1L;
        Member member = Member.builder()
            .id(memberId)
            .username("tester")
            .password("secret")
            .email("tester@example.com")
            .nickname("테스터")
            .role(Role.USER)
            .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(watchlistEntryRepository.countByMemberId(memberId)).thenReturn(20);

        assertThatThrownBy(() -> watchlistService.addEntry(memberId, "bitcoin", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("최대 20개");
    }

    private CoinMarketDto sampleMarket(String id, String symbol, BigDecimal price, BigDecimal change, BigDecimal volume) {
        return new CoinMarketDto(
            id,
            symbol,
            symbol.toUpperCase(),
            "https://assets.coingecko.com/" + id + ".png",
            price,
            BigDecimal.ZERO,
            volume,
            change
        );
    }
}
