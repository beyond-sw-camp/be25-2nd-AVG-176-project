package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.FindPasswordRequest;
import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.PasswordChangeRequest;
import com.example.team3Project.domain.user.dto.SignupRequest;
import com.example.team3Project.domain.user.dto.UserUpdateFormRequest;
import com.example.team3Project.domain.user.dto.UserWithdrawRequest;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginForm(@RequestParam(defaultValue = "/") String redirectURL,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String username,
                            Model model) {
        if (!model.containsAttribute("loginRequest")) {
            LoginRequest loginRequest = new LoginRequest();
            if (username != null && !username.isEmpty()) {
                loginRequest.setUsername(username);
            }
            model.addAttribute("loginRequest", loginRequest);
        }
        model.addAttribute("redirectURL", redirectURL);

        // Spring Security 인증 실패 에러 처리
        if (error != null) {
            String errorMessage = switch (error) {
                case "social" -> "소셜 로그인 계정입니다. 해당 소셜 서비스로 로그인해주세요.";
                case "locked" -> "계정이 잠겼습니다. 관리자에게 문의하세요.";
                case "password" -> "비밀번호가 일치하지 않습니다.";
                case "username" -> "아이디를 찾을 수 없습니다.";
                default -> "로그인에 실패했습니다. 다시 시도해주세요.";
            };
            model.addAttribute("loginError", errorMessage);
        }

        return "users/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new SignupRequest());
        return "users/signup";
    }

    /**
     * 아이디 중복 확인 API (AJAX용)
     */
    @GetMapping("/check-username")
    @ResponseBody
    public Map<String, Object> checkUsername(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();

        if (username == null || username.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "아이디를 입력해주세요.");
            return response;
        }

        boolean available = userService.isUsernameAvailable(username);
        response.put("available", available);
        response.put("message", available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");

        return response;
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("user") SignupRequest signupRequest,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        // 비밀번호 확인 검증
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordMismatch", "비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "users/signup";
        }

        try {
            userService.signup(signupRequest);
            log.info("회원가입 성공: username={}", signupRequest.getUsername());
            redirectAttributes.addFlashAttribute("signupSuccess", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/users/login";

        } catch (IllegalStateException e) {
            // 중복 아이디 에러를 BindingResult에 추가 (전체 에러로 표시)
            bindingResult.reject("duplicateUsername", e.getMessage());
            return "users/signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        // JWT 쿠키 삭제
        ResponseCookie deleteCookie = jwtUtil.deleteJwtCookie();
        response.addHeader("Set-Cookie", deleteCookie.toString());
        log.info("로그아웃 완료");
        return "redirect:/users/login";
    }

    // HTML 뷰 - 브라우저에서 직접 접속 (Thymeleaf)
    @GetMapping("/me")
    public String myPageView(@LoginUser User user, Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("user", user);
        return "users/me";
    }

    // [Deprecated] /api/users/me 로 통합됨 (UserApiController)
    // 프론트엔드는 GET /api/users/me 사용

    @GetMapping("/update")
    public String updateForm(@LoginUser User user, Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }
        UserUpdateFormRequest formRequest = new UserUpdateFormRequest();
        formRequest.setNickname(user.getNickname());
        formRequest.setPhoneNumber(user.getPhoneNumber());
        formRequest.setEmail(user.getEmail());
        model.addAttribute("userForm", formRequest);

        if (!model.containsAttribute("passwordChangeRequest")) {
            model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        }

        return "users/update";
    }

    @PostMapping("/update")
    public String update(@LoginUser User user,
                         @Valid @ModelAttribute("userForm") UserUpdateFormRequest formRequest,
                         BindingResult bindingResult,
                         HttpServletResponse response,
                         Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }

        if (bindingResult.hasErrors()) {
            return "users/update";
        }

        try {
            User updatedUser = userService.updateUserInfo(user.getId(), formRequest);

            // JWT 쿠키 갱신 (닉네임 변경 즉시 반영)
            String token = jwtUtil.generateToken(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getNickname()
            );
            ResponseCookie cookie = jwtUtil.createJwtCookie(token);
            response.addHeader("Set-Cookie", cookie.toString());

            log.info("사용자 정보 수정 성공: userId={}", user.getId());
            return "redirect:/users/me";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/update";
        }
    }

    @GetMapping("/delete")
    public String deleteForm(@LoginUser User user, Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("withdrawRequest", new UserWithdrawRequest());
        return "users/delete";
    }

    @PostMapping("/delete")
    public String delete(@LoginUser User user,
                         @Valid @ModelAttribute("withdrawRequest") UserWithdrawRequest withdrawRequest,
                         BindingResult bindingResult,
                         HttpServletResponse response,
                         Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }

        if (bindingResult.hasErrors()) {
            return "users/delete";
        }

        try {
            userService.deleteUser(user.getId(), withdrawRequest.getPassword());

            // JWT 쿠키 삭제
            ResponseCookie deleteCookie = jwtUtil.deleteJwtCookie();
            response.addHeader("Set-Cookie", deleteCookie.toString());

            log.info("회원 탈퇴 완료: userId={}", user.getId());
            return "redirect:/users/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/delete";
        }
    }

    @PostMapping("/update/password")
    public String changePassword(@LoginUser User user,
                                  @Valid @ModelAttribute("passwordChangeRequest") PasswordChangeRequest passwordRequest,
                                  BindingResult bindingResult,
                                  @ModelAttribute("userForm") UserUpdateFormRequest userForm,
                                  Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }

        // 새 비밀번호 일치 확인
        if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordMismatch", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeRequest", passwordRequest);
            model.addAttribute("passwordChangeError", true);
            return "users/update";
        }

        try {
            userService.changePassword(user.getId(), passwordRequest.getCurrentPassword(), passwordRequest.getNewPassword());
            model.addAttribute("passwordSuccessMessage", "비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("passwordChangeRequest", passwordRequest);
            model.addAttribute("passwordChangeError", true);
            model.addAttribute("passwordErrorMessage", e.getMessage());
        }

        return "users/update";
    }
    @GetMapping("/find-id")
    public String findIdForm() {
        return "users/find-id";
    }

    @PostMapping("/find-id")
    public String findId(@RequestParam("email") String email, Model model) {
        try {
            List<String> loginIds = userService.findAllLoginIdsByEmail(email);

            if (loginIds.isEmpty()) {
                model.addAttribute("errorMessage", "일치하는 회원 정보가 없습니다.");
            } else if (loginIds.size() == 1) {
                model.addAttribute("successMessage", "회원님의 아이디는 [" + loginIds.get(0) + "] 입니다.");
                model.addAttribute("foundLoginIds", loginIds);
                model.addAttribute("singleResult", true);
            } else {
                model.addAttribute("successMessage", "해당 이메일로 가입된 아이디가 " + loginIds.size() + "개 있습니다.");
                model.addAttribute("foundLoginIds", loginIds);
                model.addAttribute("multipleResults", true);
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "users/find-id";
    }

    @GetMapping("/reset-pw")
    public String resetPasswordForm() {
        return "users/reset-pw";
    }

    @PostMapping("/reset-pw")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody FindPasswordRequest request) {
        try {
            userService.resetPassword(request.getLoginId(), request.getEmail());
            return ResponseEntity.ok()
                    .body(new ApiResponse(true, "입력하신 이메일로 임시 비밀번호가 전송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "비밀번호 재설정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    // 간단한 응답 DTO
    @Data
    @AllArgsConstructor
    public static class ApiResponse {
        private boolean success;
        private String message;
    }
}
