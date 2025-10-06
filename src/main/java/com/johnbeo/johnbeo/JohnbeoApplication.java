package com.johnbeo.johnbeo;

import com.johnbeo.johnbeo.cryptodata.config.CoinGeckoProperties;
import com.johnbeo.johnbeo.cryptodata.config.TradingViewProperties;
import com.johnbeo.johnbeo.domain.watchlist.config.WatchlistProperties;
import com.johnbeo.johnbeo.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableConfigurationProperties({JwtProperties.class, CoinGeckoProperties.class, TradingViewProperties.class, WatchlistProperties.class})
public class JohnbeoApplication {

	public static void main(String[] args) {
		SpringApplication.run(JohnbeoApplication.class, args);
	}

}
