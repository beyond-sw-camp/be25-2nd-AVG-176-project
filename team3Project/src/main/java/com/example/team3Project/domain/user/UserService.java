package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.SignupRequest;
import com.example.team3Project.global.exception.LoginErrorType;
import com.example.team3Project.global.exception.LoginException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());

        return userRepository.save(user);
    }

    @Transactional
    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new LoginException(LoginErrorType.USERNAME_NOT_FOUND));

        if (user.isLocked()) {
            log.warn("로그인 시도 - 잠긴 계정: username={}", request.getUsername());
            throw new LoginException(LoginErrorType.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.increaseLoginFailCount();
            log.warn("로그인 실패 - 비밀번호 불일치: username={}, 실패횟수={}", 
                    request.getUsername(), user.getLoginFailCount());
            
            if (user.isLocked()) {
                log.warn("계정 잠김 처리: username={}", request.getUsername());
                throw new LoginException(LoginErrorType.ACCOUNT_LOCKED);
            }
            
            throw new LoginException(LoginErrorType.PASSWORD_MISMATCH);
        }

        if (user.getLoginFailCount() > 0) {
            user.resetLoginFailCount();
        }
        
        log.info("로그인 성공: username={}", request.getUsername());
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 최소 8자 이상이어야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    @Transactional
    public void unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.unlock();
        log.info("계정 잠김 해제: userId={}", userId);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
