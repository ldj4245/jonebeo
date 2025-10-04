package com.johnbeo.johnbeo.security.jwt;

import jakarta.servlet.http.Cookie;

public final class JwtCookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "johnbeo_access_token";

    private JwtCookieUtils() {
    }

    public static Cookie createAccessTokenCookie(String token, long maxAgeSeconds, boolean secure) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge((int) Math.min(maxAgeSeconds, Integer.MAX_VALUE));
        return cookie;
    }

    public static Cookie createClearingCookie(boolean secure) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
}
