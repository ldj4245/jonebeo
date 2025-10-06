package com.johnbeo.johnbeo.domain.watchlist.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "watchlist")
public class WatchlistProperties {

    private BigDecimal usdToKrwRate = BigDecimal.valueOf(1350);
    private final List<DefaultCoin> defaults = new ArrayList<>();

    public BigDecimal getUsdToKrwRate() {
        return usdToKrwRate;
    }

    public void setUsdToKrwRate(BigDecimal usdToKrwRate) {
        if (usdToKrwRate != null && usdToKrwRate.signum() > 0) {
            this.usdToKrwRate = usdToKrwRate;
        }
    }

    public List<DefaultCoin> getDefaults() {
        return Collections.unmodifiableList(defaults);
    }

    public void setDefaults(List<DefaultCoin> defaults) {
        this.defaults.clear();
        if (defaults == null) {
            return;
        }
        defaults.stream()
            .filter(Objects::nonNull)
            .map(DefaultCoin::normalize)
            .filter(Objects::nonNull)
            .forEach(this.defaults::add);
    }

    public List<String> defaultCoinIds() {
        return defaults.stream()
            .map(DefaultCoin::coinId)
            .toList();
    }

    public String findDefaultLabel(String coinId) {
        if (!StringUtils.hasText(coinId)) {
            return null;
        }
        return defaults.stream()
            .filter(defaultCoin -> defaultCoin.coinId.equalsIgnoreCase(coinId))
            .map(DefaultCoin::label)
            .findFirst()
            .orElse(null);
    }

    public record DefaultCoin(String coinId, String label) {

        private static DefaultCoin normalize(DefaultCoin source) {
            if (source == null) {
                return null;
            }
            String coinId = normalizeId(source.coinId);
            String label = source.label != null ? source.label.trim() : null;
            if (!StringUtils.hasText(coinId)) {
                return null;
            }
            return new DefaultCoin(coinId, label);
        }

        private static String normalizeId(String coinId) {
            if (!StringUtils.hasText(coinId)) {
                return null;
            }
            return coinId.trim().toLowerCase(Locale.ROOT);
        }
    }
}
