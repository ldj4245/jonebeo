package com.johnbeo.johnbeo.domain.post.service.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class PostViewTracker {

    private final @Qualifier("postViewCache") Cache<String, Boolean> postViewCache;

    public boolean shouldCountView(Long postId, MemberPrincipal principal) {
        if (postId == null) {
            return false;
        }
        String viewerKey = resolveViewerKey(principal);
        if (!StringUtils.hasText(viewerKey)) {
            return true;
        }
        String cacheKey = postId + "::" + viewerKey;
        Boolean existing = postViewCache.asMap().putIfAbsent(cacheKey, Boolean.TRUE);
        return existing == null;
    }

    private String resolveViewerKey(MemberPrincipal principal) {
        if (principal != null) {
            return "member:" + principal.getId();
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String ip = extractClientIp(request);
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
        return "guest:" + ip + ":" + userAgent.hashCode();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }
}
