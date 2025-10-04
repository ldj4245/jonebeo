package com.johnbeo.johnbeo.auth.service;

import com.johnbeo.johnbeo.auth.dto.AuthResponse;
import com.johnbeo.johnbeo.auth.dto.LoginRequest;
import com.johnbeo.johnbeo.auth.dto.RegisterRequest;
import com.johnbeo.johnbeo.common.exception.ResourceAlreadyExistsException;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.security.jwt.JwtTokenProvider;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void register(RegisterRequest request) {
        if (memberRepository.existsByUsername(request.username())) {
            throw new ResourceAlreadyExistsException("이미 사용 중인 사용자명입니다.");
        }
        if (memberRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("이미 등록된 이메일입니다.");
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new ResourceAlreadyExistsException("이미 사용 중인 닉네임입니다.");
        }

        Member member = Member.builder()
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .email(request.email())
            .nickname(request.nickname())
            .role(Role.USER)
            .build();
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        return new AuthResponse("Bearer", accessToken, jwtTokenProvider.getAccessTokenTtl(), refreshToken);
    }
}
