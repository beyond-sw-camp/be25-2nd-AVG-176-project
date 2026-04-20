package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.PasswordChangeRequest;
import com.example.team3Project.domain.user.dto.UserResponse;
import com.example.team3Project.domain.user.dto.UserUpdateFormRequest;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> me(@LoginUser User user) {
        if (user == null) {
            log.warn("인증되지 않은 사용자의 내 정보 조회 시도");
            return ResponseEntity.status(401).body(Map.of(
                    "error", "로그인이 필요합니다.",
                    "code", "UNAUTHORIZED"
            ));
        }

        log.info("내 정보 조회 성공: userId={}, username={}", user.getId(), user.getUsername());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@LoginUser User user,
                                      @Valid @RequestBody UserUpdateFormRequest request) {
        if (user == null) {
            log.warn("인증되지 않은 사용자의 정보 수정 시도");
            return ResponseEntity.status(401).body(Map.of(
                    "error", "로그인이 필요합니다.",
                    "code", "UNAUTHORIZED"
            ));
        }

        try {
            User updatedUser = userService.updateUserInfo(user.getId(), request);
            log.info("사용자 정보 수정 성공: userId={}", user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "정보가 성공적으로 수정되었습니다.",
                    "user", UserResponse.from(updatedUser)
            ));
        } catch (IllegalArgumentException e) {
            log.warn("사용자 정보 수정 실패: userId={}, reason={}", user.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@LoginUser User user,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        if (user == null) {
            log.warn("인증되지 않은 사용자의 비밀번호 변경 시도");
            return ResponseEntity.status(401).body(Map.of(
                    "error", "로그인이 필요합니다.",
                    "code", "UNAUTHORIZED"
            ));
        }

        // 새 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
            ));
        }

        try {
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
            log.info("비밀번호 변경 성공: userId={}", user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호가 성공적으로 변경되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            log.warn("비밀번호 변경 실패: userId={}, reason={}", user.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        try {
            List<String> loginIds = userService.findAllLoginIdsByEmail(email);

            if (loginIds.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "일치하는 회원 정보가 없습니다."
                ));
            }

            String message = loginIds.size() == 1
                    ? "회원님의 아이디를 찾았습니다."
                    : "해당 이메일로 가입된 아이디가 " + loginIds.size() + "개 있습니다.";

            log.info("아이디 찾기 성공: email={}, count={}", email, loginIds.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message,
                    "loginIds", loginIds
            ));

        } catch (IllegalArgumentException e) {
            log.warn("아이디 찾기 실패: email={}, reason={}", email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/reset-pw")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String loginId = request.get("loginId");
        String email = request.get("email");

        log.debug("API 비밀번호 재설정 요청: loginId={}, email={}", loginId, email);

        if (loginId == null || loginId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디를 입력해주세요."
            ));
        }

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
            ));
        }

        try {
            userService.resetPassword(loginId, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "입력하신 이메일로 임시 비밀번호가 전송되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            log.warn("API 비밀번호 재설정 실패: loginId={}, email={}, reason={}", loginId, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            // 이메일 발송 실패 등 비즈니스 예외
            log.error("API 비밀번호 재설정 - 이메일 발송 실패: loginId={}, email={}, reason={}", loginId, email, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "EMAIL_SEND_FAILED"
            ));
        } catch (RuntimeException e) {
            // 기타 예외 (DB 오류 등)
            log.error("API 비밀번호 재설정 - 서버 오류: loginId={}, email={}, error={}", loginId, email, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "비밀번호 재설정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    "errorCode", "INTERNAL_ERROR"
            ));
        }
    }
}
