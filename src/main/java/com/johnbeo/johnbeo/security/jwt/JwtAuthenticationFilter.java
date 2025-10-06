package com.johnbeo.johnbeo.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    }
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ex) {
                log.warn("인증 토큰의 사용자 정보를 찾지 못했습니다: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
                response.addCookie(JwtCookieUtils.createClearingCookie(cookieSecure));
            } catch (Exception ex) {
                log.error("JWT 인증 처리 중 오류가 발생했습니다", ex);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        Cookie cookie = WebUtils.getCookie(request, JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }
        return null;
    }
}
