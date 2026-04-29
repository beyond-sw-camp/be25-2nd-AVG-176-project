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

import java.util.List;
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

    /**
     * 아이디 중복 확인
     */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByUsername(username);
    }

    @Transactional
    public User signup(SignupRequest request) {
        // username 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        User user = new User();
        user.setLoginId(request.getUsername());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setNickname(request.getNickname());
        user.setProvider(User.AuthProvider.LOCAL);

        return userRepository.save(user);
    }

    @Transactional
    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new LoginException(LoginErrorType.USERNAME_NOT_FOUND));

        // 소셜 로그인 사용자는 일반 로그인 불가
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 계정입니다. 해당 소셜 서비스로 로그인해주세요.");
        }

        if (user.isLocked()) {
            log.warn("로그인 시도 - 잠긴 계정: username={}", request.getUsername());
            throw new LoginException(LoginErrorType.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.increaseLoginFailCount();
            log.warn("로그인 실패 - 비밀번호 불일치 username={}, 실패횟수={}",
                    request.getUsername(), user.getLoginFailCount());

            if (user.isLocked()) {
                log.warn("계정 잠금 처리: username={}", request.getUsername());
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

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

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
        log.info("계정 잠금 해제: userId={}", userId);
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
    public User updateUserInfo(Long userId, UserUpdateFormRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        log.info("사용자 프로필 수정 완료: userId={}", userId);
        return user;
    }

    public void sendPasswordResetCode(String loginIdOrEmail) {
        User user = findUserByLoginIdOrEmail(loginIdOrEmail)
                .orElseThrow(() -> new LoginException(LoginErrorType.USER_NOT_FOUND));

        // 소셜 로그인 사용자는 비밀번호 재설정 불가
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호 재설정이 불가능합니다. 해당 소셜 서비스를 이용해주세요.");
        }

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

        // 일반 로그인 사용자는 비밀번호 확인 필요
        if (!user.isSocialUser()) {
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }
        // 소셜 로그인 사용자는 비밀번호 확인 없이 바로 삭제 가능

        userRepository.delete(user);
        log.info("회원 탈퇴 완료: userId={}", userId);
    }

    /**
     * [Deprecated] 단일 사용자 조회용. 여러 사용자가 같은 이메일을 가질 수 있어 findAllLoginIdsByEmail 사용 권장
     */
    @Deprecated
    public String findUsernameByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return user.getUsername();
    }

    /**
     * 이메일로 모든 로그인 아이디 조회 (같은 이메일로 여러 계정 가입 가능)
     */
    public List<String> findAllLoginIdsByEmail(String email) {
        List<User> users = userRepository.findAllByEmail(email);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("일치하는 회원 정보가 없습니다.");
        }
        return users.stream()
                .map(User::getLoginId)
                .distinct()
                .toList();
    }

    @Transactional
    public void resetPassword(String loginId, String email) {
        log.info("[비밀번호 재설정] STEP 1 - 요청 수신: loginId={}, email={}", loginId, email);

        // STEP 2: 사용자 조회
        log.debug("[비밀번호 재설정] STEP 2 - 사용자 조회 시작");
        User user = userRepository.findByLoginIdAndEmail(loginId, email)
                .orElseThrow(() -> {
                    log.warn("[비밀번호 재설정] STEP 2 - 사용자 조회 실패: loginId={}, email={}", loginId, email);
                    return new IllegalArgumentException("아이디와 이메일 정보를 다시 확인해주세요.");
                });
        log.info("[비밀번호 재설정] STEP 2 - 사용자 조회 성공: userId={}", user.getId());

        // STEP 3: 소셜 로그인 사용자 체크
        log.debug("[비밀번호 재설정] STEP 3 - 소셜 로그인 여부 체크");
        if (user.isSocialUser()) {
            log.warn("[비밀번호 재설정] STEP 3 - 소셜 로그인 사용자 차단: loginId={}", loginId);
            throw new IllegalArgumentException("소셜 로그인 사용자는 임시 비밀번호를 발급받을 수 없습니다.");
        }
        log.info("[비밀번호 재설정] STEP 3 - 소셜 로그인 여부 체크 완료 (일반 사용자)");

        // STEP 4: 임시 비밀번호 생성
        log.debug("[비밀번호 재설정] STEP 4 - 임시 비밀번호 생성");
        String tempPassword = createTempPassword();
        log.info("[비밀번호 재설정] STEP 4 - 임시 비밀번호 생성 완료");

        // STEP 5: 비밀번호 암호화 및 DB 저장
        log.debug("[비밀번호 재설정] STEP 5 - 비밀번호 암호화 및 DB 저장");
        try {
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.resetLoginFailCount();
            if (user.isLocked()) {
                user.unlock();
            }
            // @Transactional로 인해 여기서 DB 저장
            log.info("[비밀번호 재설정] STEP 5 - DB 저장 완료 (트랜잭션 내)");
        } catch (Exception e) {
            log.error("[비밀번호 재설정] STEP 5 - DB 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("비밀번호 저장 중 오류가 발생했습니다.", e);
        }

        // STEP 6: 이메일 발송
        log.debug("[비밀번호 재설정] STEP 6 - 이메일 발송 시작: to={}", user.getEmail());
        try {
            emailService.sendTemporaryPassword(user.getEmail(), tempPassword);
            log.info("[비밀번호 재설정] STEP 6 - 이메일 발송 완료");
        } catch (Exception e) {
            log.error("[비밀번호 재설정] STEP 6 - 이메일 발송 실패: error={}", e.getMessage(), e);
            // 이메일 실패는 비즈니스 예외로 처리 - DB는 이미 저장됨
            throw new IllegalStateException("임시 비밀번호는 생성되었으나 이메일 발송에 실패했습니다. 관리자에게 문의해주세요. (이메일: " + user.getEmail() + ")", e);
        }

        log.info("[비밀번호 재설정] STEP 7 - 전체 프로세스 완료: userId={}, loginId={}, email={}",
                user.getId(), user.getLoginId(), user.getEmail());
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
