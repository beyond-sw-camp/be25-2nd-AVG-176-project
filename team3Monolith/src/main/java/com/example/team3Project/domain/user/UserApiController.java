package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.PasswordResetCodeRequest;
import com.example.team3Project.domain.user.dto.PasswordResetRequest;
import com.example.team3Project.domain.user.dto.PasswordResetVerifyRequest;
import com.example.team3Project.domain.user.dto.UserResponse;
import com.example.team3Project.domain.user.dto.UserUpdateRequest;
import com.example.team3Project.domain.user.dto.UserWithdrawRequest;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
// Vue 같은 분리 프론트가 사용하는 사용자 JSON API를 제공한다.
public class UserApiController {

    private static final String ACCESS_TOKEN_COOKIE = "token";

    private final UserService userService;

    @PostMapping("/reset-pw/code")
    public ResponseEntity<Void> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request) {
        userService.sendPasswordResetCode(request.getLoginIdOrEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-pw/verify")
    public ResponseEntity<Void> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        userService.verifyPasswordResetCode(request.getLoginIdOrEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reset-pw")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(
                request.getLoginIdOrEmail(),
                request.getCode(),
                request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // REST 로그아웃도 MVC와 같은 방식으로 JWT 쿠키를 만료시킨다.
        expireAccessTokenCookie(response);
        log.info("로그아웃 완료");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        // 프론트 앱이 시작될 때 현재 로그인 사용자 정보를 확인할 수 있도록 내려준다.
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(
            @LoginUser User user,
            @Valid @RequestBody UserUpdateRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User updatedUser = userService.updateUser(user.getId(), request);
        return ResponseEntity.ok(UserResponse.from(updatedUser));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(
            @LoginUser User user,
            @Valid @RequestBody UserWithdrawRequest request,
            HttpServletResponse response) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        userService.deleteUser(user.getId(), request.getPassword());
        // 회원 탈퇴 후에는 남아 있는 access token 쿠키도 즉시 제거한다.
        expireAccessTokenCookie(response);
        log.info("회원 탈퇴 완료: userId={}", user.getId());
        return ResponseEntity.ok().build();
    }

    private void expireAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
