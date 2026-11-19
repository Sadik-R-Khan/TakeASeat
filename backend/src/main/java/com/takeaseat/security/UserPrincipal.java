package com.takeaseat.security;

import com.takeaseat.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            Long id,
            String name,
            String email,
            String password,
            Role role,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
