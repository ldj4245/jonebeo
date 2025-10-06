package com.johnbeo.johnbeo.auth.repository;

import com.johnbeo.johnbeo.auth.entity.RefreshToken;
import com.johnbeo.johnbeo.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByMember(Member member);
}
