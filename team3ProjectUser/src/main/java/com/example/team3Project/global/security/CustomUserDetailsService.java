package com.example.team3Project.global.security;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Spring Security 인증 시도: username={}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 소셜 로그인 사용자는 일반 로그인 불가 (password가 null 또는 empty)
        if (user.isSocialUser()) {
            log.warn("소셜 로그인 계정으로 일반 로그인 시도: {}", username);
            throw new UsernameNotFoundException("소셜 로그인 계정입니다. 해당 소셜 서비스로 로그인해주세요.");
        }

        // 계정 잠금 확인
        if (user.isLocked()) {
            log.warn("잠긴 계정 로그인 시도: {}", username);
            throw new UsernameNotFoundException("계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                !user.isLocked(), // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
