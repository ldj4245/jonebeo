package com.johnbeo.johnbeo.cryptodata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "coingecko")
public class CoinGeckoProperties {

    private String baseUrl = "https://api.coingecko.com/api/v3";
    private Market market = new Market();
    private SimplePrice simplePrice = new SimplePrice();

    @Getter
    @Setter
    public static class Market {
        private String vsCurrency = "usd";
        private int perPage = 100;
    }

    @Getter
    @Setter
    public static class SimplePrice {
        private String vsCurrency = "usd";
        private boolean include24hChange = true;
    }
}
