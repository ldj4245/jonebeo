package com.johnbeo.johnbeo.security.jwt;

import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import com.johnbeo.johnbeo.security.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final CustomUserDetailsService userDetailsService;

    private Key signingKey;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(MemberPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.accessTokenExpiration());
        return buildToken(principal, Date.from(now), Date.from(expiry));
    }

    public String generateRefreshToken(MemberPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.refreshTokenExpiration());
        return buildToken(principal, Date.from(now), Date.from(expiry));
    }

    public Authentication getAuthentication(String token) {
        Long memberId = Long.parseLong(parseClaims(token).getBody().getSubject());
        MemberPrincipal principal = userDetailsService.loadUserById(memberId);
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public long getAccessTokenTtl() {
        return jwtProperties.accessTokenExpiration();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getBody().getSubject());
    }

    public Instant getExpiration(String token) {
        return parseClaims(token).getBody().getExpiration().toInstant();
    }

    private String buildToken(MemberPrincipal principal, Date issuedAt, Date expiration) {
        return Jwts.builder()
            .setSubject(principal.getId().toString())
            .claim("username", principal.getUsername())
            .claim("role", principal.getRole().name())
            .setIssuedAt(issuedAt)
            .setExpiration(expiration)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact();
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token);
    }
}
