package com.johnbeo.johnbeo.auth.service;

import com.johnbeo.johnbeo.auth.entity.RefreshToken;
import com.johnbeo.johnbeo.auth.repository.RefreshTokenRepository;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken save(Member member, String token, Instant expiresAt) {
        refreshTokenRepository.deleteByMember(member);
        RefreshToken refreshToken = RefreshToken.builder()
            .member(member)
            .token(token)
            .expiresAt(expiresAt)
            .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken getValidToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("유효하지 않은 리프레시 토큰입니다."));
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("만료된 리프레시 토큰입니다.");
        }
        return refreshToken;
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteByMember(Member member) {
        refreshTokenRepository.deleteByMember(member);
    }
}
