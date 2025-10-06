package com.johnbeo.johnbeo.cryptodata.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "tradingview")
public class TradingViewProperties {

    private boolean enabled = false;
    private String defaultExchange = "BINANCE";
    private String defaultQuote = "USDT";
    private Map<String, String> symbols = new HashMap<>();
    private boolean useDefaultMapping = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultExchange() {
        return defaultExchange;
    }

    public void setDefaultExchange(String defaultExchange) {
        if (StringUtils.hasText(defaultExchange)) {
            this.defaultExchange = defaultExchange.trim();
        }
    }

    public String getDefaultQuote() {
        return defaultQuote;
    }

    public void setDefaultQuote(String defaultQuote) {
        if (StringUtils.hasText(defaultQuote)) {
            this.defaultQuote = defaultQuote.trim();
        }
    }

    public Map<String, String> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, String> symbols) {
        this.symbols = new HashMap<>();
        if (symbols == null) {
            return;
        }
        symbols.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                this.symbols.put(key.toLowerCase(Locale.ROOT), value.trim());
            }
        });
    }

    public boolean isUseDefaultMapping() {
        return useDefaultMapping;
    }

    public void setUseDefaultMapping(boolean useDefaultMapping) {
        this.useDefaultMapping = useDefaultMapping;
    }

    public Optional<String> resolveSymbol(String coinId, String symbol) {
        if (!enabled) {
            return Optional.empty();
        }
        String mapped = findMappedSymbol(coinId, symbol);
        if (StringUtils.hasText(mapped)) {
            return Optional.of(mapped);
        }
        if (!useDefaultMapping) {
            return Optional.empty();
        }
        String fallback = buildDefaultSymbol(symbol);
        return StringUtils.hasText(fallback) ? Optional.of(fallback) : Optional.empty();
    }

    private String findMappedSymbol(String coinId, String symbol) {
        if (symbols.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(coinId)) {
            String mapped = symbols.get(coinId.toLowerCase(Locale.ROOT));
            if (StringUtils.hasText(mapped)) {
                return mapped;
            }
        }
        if (StringUtils.hasText(symbol)) {
            return symbols.get(symbol.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private String buildDefaultSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)
            || !StringUtils.hasText(defaultExchange)
            || !StringUtils.hasText(defaultQuote)) {
            return null;
        }
        return defaultExchange.toUpperCase(Locale.ROOT)
            + ':'
            + symbol.trim().toUpperCase(Locale.ROOT)
            + defaultQuote.toUpperCase(Locale.ROOT);
    }
}
