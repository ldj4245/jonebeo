package com.johnbeo.johnbeo.security.service;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.repository.MemberRepository;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Member not found with username: " + username));
        return MemberPrincipal.from(member);
    }

    @Transactional(readOnly = true)
    public MemberPrincipal loadUserById(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Member not found with id: " + id));
        return MemberPrincipal.from(member);
    }
}
