package com.johnbeo.johnbeo.security.model;

import com.johnbeo.johnbeo.domain.member.entity.Member;
import com.johnbeo.johnbeo.domain.member.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final String nickname;

    public static MemberPrincipal from(Member member) {
        return new MemberPrincipal(
            member.getId(),
            member.getUsername(),
            member.getPassword(),
            member.getRole(),
            member.getNickname()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberPrincipal that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
