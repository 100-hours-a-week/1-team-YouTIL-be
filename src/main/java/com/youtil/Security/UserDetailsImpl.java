package com.youtil.Security;


import com.youtil.Model.User;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailsImpl implements UserDetails {
    private final User user;

    public UserDetailsImpl(User userJpa) {
        this.user = userJpa;
    }

    public long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 현재 권한이 없지만, 필요하면 추가 가능
    }

    @Override
    public String getPassword() {
        return "";
    }


    @Override
    public String getUsername() {
        return user.getNickname();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부, 필요에 따라 변경 가능
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠김 여부, 필요에 따라 변경 가능
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 여부, 필요에 따라 변경 가능
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부, 필요에 따라 변경 가능
    }
}
