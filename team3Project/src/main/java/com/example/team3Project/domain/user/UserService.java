package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.SignupRequest;
import com.example.team3Project.domain.user.dto.UserUpdateFormRequest;
import com.example.team3Project.domain.user.dto.UserUpdateRequest;
import com.example.team3Project.global.exception.LoginErrorType;
import com.example.team3Project.global.exception.LoginException;
import com.example.team3Project.global.util.EmailService;
import com.example.team3Project.global.util.VerificationCodeStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeStore verificationCodeStore;
    private final EmailService emailService;

    @Transactional
    public User signup(SignupRequest request) {
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setName(request.getNickname());
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

    @Transactional
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setNickname(request.getNickname());
        log.info("사용자 정보 수정 완료: userId={}", userId);
        return user;
    }

    @Transactional
    public void updateUserInfo(Long userId, UserUpdateFormRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setName(request.getNickname());
        log.info("사용자 프로필 수정 완료: userId={}", userId);
    }

    public void sendPasswordResetCode(String loginIdOrEmail) {
        User user = findUserByLoginIdOrEmail(loginIdOrEmail)
                .orElseThrow(() -> new LoginException(LoginErrorType.USER_NOT_FOUND));

        String code = verificationCodeStore.generateAndStore(user.getEmail());
        emailService.sendVerificationCode(user.getEmail(), code);
        log.info("비밀번호 재설정 인증코드 발송: userId={}, email={}", user.getId(), user.getEmail());
    }

    public void verifyPasswordResetCode(String loginIdOrEmail, String code) {
        User user = findUserByLoginIdOrEmail(loginIdOrEmail)
                .orElseThrow(() -> new LoginException(LoginErrorType.USER_NOT_FOUND));

        boolean valid = verificationCodeStore.verifyAndConsume(user.getEmail(), code);
        if (!valid) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 인증코드입니다.");
        }
        verificationCodeStore.markAsVerified(user.getEmail());
        log.info("비밀번호 재설정 인증코드 검증 성공: userId={}", user.getId());
    }

    @Transactional
    public void resetPassword(String loginIdOrEmail, String code, String newPassword) {
        User user = findUserByLoginIdOrEmail(loginIdOrEmail)
                .orElseThrow(() -> new LoginException(LoginErrorType.USER_NOT_FOUND));

        boolean valid = verificationCodeStore.verifyAndConsume(user.getEmail(), code);
        if (!valid) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 인증코드입니다.");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 최소 8자 이상이어야 합니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.resetLoginFailCount();
        if (user.isLocked()) {
            user.unlock();
        }
        log.info("비밀번호 재설정 완료: userId={}", user.getId());
    }

    @Transactional
    public void deleteUser(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LoginException(LoginErrorType.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        userRepository.delete(user);
        log.info("회원 탈퇴 완료: userId={}", userId);
    }

    public String findUsernameByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return user.getUsername();
    }

    @Transactional
    public void resetPassword(String username, String email) {
        User user = userRepository.findByUsernameAndEmail(username, email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));

        String tempPassword = createTempPassword();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.resetLoginFailCount();
        if (user.isLocked()) {
            user.unlock();
        }

        emailService.sendTemporaryPassword(user.getEmail(), tempPassword);
        log.info("임시 비밀번호 발급 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    private Optional<User> findUserByLoginIdOrEmail(String loginIdOrEmail) {
        Optional<User> byUsername = userRepository.findByUsername(loginIdOrEmail);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return userRepository.findByEmail(loginIdOrEmail);
    }

    private String createTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}