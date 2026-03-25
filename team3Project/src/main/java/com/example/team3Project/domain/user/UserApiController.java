package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.PasswordResetCodeRequest;
import com.example.team3Project.domain.user.dto.PasswordResetRequest;
import com.example.team3Project.domain.user.dto.PasswordResetVerifyRequest;
import com.example.team3Project.domain.user.dto.UserResponse;
import com.example.team3Project.domain.user.dto.UserUpdateRequest;
import com.example.team3Project.domain.user.dto.UserWithdrawRequest;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.exception.LoginException;
import com.example.team3Project.global.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
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
public class UserApiController {

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
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SessionUtils.invalidateSession(request);
        log.info("로그아웃 완료");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
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
            HttpServletRequest httpRequest) {
        
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        userService.deleteUser(user.getId(), request.getPassword());
        SessionUtils.invalidateSession(httpRequest);
        log.info("회원 탈퇴 및 세션 무효화: userId={}", user.getId());
        return ResponseEntity.ok().build();
    }
}
