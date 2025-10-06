package com.johnbeo.johnbeo.domain.watchlist.service;

import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.cryptodata.dto.CoinMarketDto;
import com.johnbeo.johnbeo.cryptodata.service.CryptoDataService;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.domain.watchlist.config.WatchlistProperties;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistEntryResponse;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistItemResponse;
import com.johnbeo.johnbeo.domain.watchlist.dto.WatchlistView;
import com.johnbeo.johnbeo.domain.watchlist.entity.WatchlistEntry;
import com.johnbeo.johnbeo.domain.watchlist.repository.WatchlistEntryRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private static final MathContext PREMIUM_MATH = new MathContext(6, RoundingMode.HALF_UP);
    private static final int MAX_ENTRIES = 20;

    private final WatchlistEntryRepository watchlistEntryRepository;
    private final MemberRepository memberRepository;
    private final CryptoDataService cryptoDataService;
    private final WatchlistProperties watchlistProperties;

    @Transactional(readOnly = true)
    public WatchlistView loadWatchlist(MemberPrincipal principal) {
        WatchlistSource source = resolveSource(principal);
        if (source.coinIds().isEmpty()) {
            return new WatchlistView(List.of(), source.usingDefault());
        }

        Map<String, CoinMarketDto> usdMarkets = toMarketMap(
            cryptoDataService.getMarketCoinsByIds(source.coinIds(), "usd"));
        Map<String, CoinMarketDto> krwMarkets = toMarketMap(
            cryptoDataService.getMarketCoinsByIds(source.coinIds(), "krw"));

        List<WatchlistItemResponse> items = new ArrayList<>();
        for (String coinId : source.coinIds()) {
            CoinMarketDto usd = usdMarkets.get(coinId);
            CoinMarketDto krw = krwMarkets.get(coinId);
            if (usd == null && krw == null) {
                continue;
            }
            BigDecimal priceUsd = usd != null ? usd.currentPrice() : null;
            BigDecimal priceKrw = krw != null ? krw.currentPrice() : null;
            BigDecimal change24h = usd != null ? usd.priceChangePercentage24h() : krw != null ? krw.priceChangePercentage24h() : null;
            BigDecimal volumeUsd = usd != null ? usd.totalVolume() : null;
            BigDecimal premium = computePremium(priceUsd, priceKrw);

            String label = source.labels().get(coinId);
            if (!StringUtils.hasText(label)) {
                label = Optional.ofNullable(watchlistProperties.findDefaultLabel(coinId)).orElse(null);
            }

            String name = firstNonBlank(
                usd != null ? usd.name() : null,
                krw != null ? krw.name() : null,
                label
            );
            if (!StringUtils.hasText(label)) {
                label = name;
            }

            WatchlistItemResponse item = new WatchlistItemResponse(
                coinId,
                firstNonBlank(usd != null ? usd.symbol() : null, krw != null ? krw.symbol() : null, coinId),
                name,
                label,
                firstNonBlank(usd != null ? usd.image() : null, krw != null ? krw.image() : null, null),
                priceKrw,
                priceUsd,
                change24h,
                premium,
                volumeUsd,
                source.customCoinIds().contains(coinId)
            );
            items.add(item);
        }
        return new WatchlistView(items, source.usingDefault());
    }

    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> listEntries(Long memberId) {
        List<WatchlistEntry> entries = watchlistEntryRepository.findByMemberIdOrderByDisplayOrderAscIdAsc(memberId);
        return entries.stream()
            .map(entry -> new WatchlistEntryResponse(entry.getId(), entry.getCoinId(), entry.getLabel(), entry.getDisplayOrder()))
            .toList();
    }

    @Transactional
    public void addEntry(Long memberId, String coinId, String label) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        String normalizedCoinId = normalizeCoinId(coinId);
        if (!StringUtils.hasText(normalizedCoinId)) {
            throw new IllegalArgumentException("coinId must not be blank");
        }
        if (watchlistEntryRepository.countByMemberId(memberId) >= MAX_ENTRIES) {
            throw new IllegalStateException("관심 코인 목록은 최대 " + MAX_ENTRIES + "개까지 등록할 수 있습니다.");
        }
        if (watchlistEntryRepository.existsByMemberIdAndCoinIdIgnoreCase(memberId, normalizedCoinId)) {
            throw new ResourceAlreadyExistsException("Coin already exists in watchlist");
        }

        // Validate coin by attempting to load market info
        List<CoinMarketDto> validation = cryptoDataService.getMarketCoinsByIds(List.of(normalizedCoinId), "usd");
        if (validation == null || validation.isEmpty()) {
            throw new ResourceNotFoundException("Invalid coin id: " + normalizedCoinId);
        }

        int nextOrder = watchlistEntryRepository.findMaxDisplayOrder(memberId) + 1;
        WatchlistEntry entry = WatchlistEntry.builder()
            .member(member)
            .coinId(normalizedCoinId)
            .label(StringUtils.hasText(label) ? label.trim() : null)
            .displayOrder(nextOrder)
            .build();
        watchlistEntryRepository.save(entry);
    }

    @Transactional
    public void removeEntry(Long memberId, Long entryId) {
        WatchlistEntry entry = watchlistEntryRepository.findByIdAndMemberId(entryId, memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry not found"));
        watchlistEntryRepository.delete(entry);
    }

    private WatchlistSource resolveSource(MemberPrincipal principal) {
        if (principal == null) {
            return WatchlistSource.fromDefaults(watchlistProperties.defaultCoinIds());
        }
        List<WatchlistEntry> entries = watchlistEntryRepository.findByMemberIdOrderByDisplayOrderAscIdAsc(principal.getId());
        if (entries.isEmpty()) {
            return WatchlistSource.fromDefaults(watchlistProperties.defaultCoinIds());
        }
        List<String> coinIds = entries.stream()
            .map(WatchlistEntry::getCoinId)
            .filter(StringUtils::hasText)
            .map(this::normalizeCoinId)
            .filter(StringUtils::hasText)
            .toList();
        Map<String, String> labels = entries.stream()
            .collect(Collectors.toMap(
                entry -> normalizeCoinId(entry.getCoinId()),
                entry -> StringUtils.hasText(entry.getLabel()) ? entry.getLabel().trim() : "",
                (left, right) -> left,
                LinkedHashMap::new
            ));
        return WatchlistSource.fromCustom(coinIds, labels);
    }

    private Map<String, CoinMarketDto> toMarketMap(List<CoinMarketDto> markets) {
        if (markets == null) {
            return Map.of();
        }
        return markets.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                dto -> normalizeCoinId(dto.id()),
                dto -> dto,
                (existing, ignored) -> existing,
                LinkedHashMap::new
            ));
    }

    private BigDecimal computePremium(BigDecimal priceUsd, BigDecimal priceKrw) {
        if (priceUsd == null || priceUsd.signum() <= 0 || priceKrw == null || priceKrw.signum() <= 0) {
            return null;
        }
        BigDecimal fx = watchlistProperties.getUsdToKrwRate();
        if (fx == null || fx.signum() <= 0) {
            return null;
        }
        BigDecimal referenceKrw = priceUsd.multiply(fx, PREMIUM_MATH);
        if (referenceKrw.signum() <= 0) {
            return null;
        }
        return priceKrw.divide(referenceKrw, PREMIUM_MATH)
            .subtract(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(100), PREMIUM_MATH);
    }

    private String normalizeCoinId(String coinId) {
        if (!StringUtils.hasText(coinId)) {
            return null;
        }
        return coinId.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record WatchlistSource(List<String> coinIds, Map<String, String> labels, boolean usingDefault, Set<String> customCoinIds) {

        private static WatchlistSource fromDefaults(List<String> defaultIds) {
            List<String> coinIds = defaultIds == null ? List.of() : defaultIds.stream()
                .filter(StringUtils::hasText)
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
            return new WatchlistSource(coinIds, Map.of(), true, Set.of());
        }

        private static WatchlistSource fromCustom(List<String> coinIds, Map<String, String> labels) {
            List<String> normalizedIds = coinIds == null ? List.of() : coinIds.stream()
                .filter(StringUtils::hasText)
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
            Map<String, String> sanitizedLabels = labels == null ? Map.of() : labels.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .collect(Collectors.toMap(
                    entry -> entry.getKey().trim().toLowerCase(Locale.ROOT),
                    entry -> StringUtils.hasText(entry.getValue()) ? entry.getValue().trim() : "",
                    (left, right) -> left,
                    LinkedHashMap::new
                ));
            Set<String> customCoinIds = new LinkedHashSet<>(normalizedIds);
            return new WatchlistSource(normalizedIds, sanitizedLabels, false, customCoinIds);
        }
    }
}
