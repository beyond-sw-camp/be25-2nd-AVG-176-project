package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.PasswordChangeRequest;
import com.example.team3Project.domain.user.dto.SignupRequest;
import com.example.team3Project.domain.user.dto.UserUpdateFormRequest;
import com.example.team3Project.domain.user.dto.UserWithdrawRequest;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.exception.LoginException;
import com.example.team3Project.global.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final String ACCESS_TOKEN_COOKIE = "token";
    private static final int ACCESS_TOKEN_MAX_AGE = 60 * 60 * 2;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${app.auth.login-url:http://100.119.201.17:9000/users/login}")
    private String loginUrl;

    @GetMapping("/login")
    public String loginForm(@RequestParam(defaultValue = "/") String redirectURL,
                            Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        model.addAttribute("redirectURL", redirectURL);
        return "users/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                        BindingResult bindingResult,
                        @RequestParam(defaultValue = "/") String redirectURL,
                        HttpServletResponse response,
                        Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("redirectURL", redirectURL);
            return "users/login";
        }

        try {
            User loginUser = userService.login(loginRequest);
            String accessToken = jwtUtil.createAccessToken(loginUser);
            addAccessTokenCookie(response, accessToken);

            log.info("로그인 성공: userId={}, redirectURL={}", loginUser.getId(), redirectURL);
            return "redirect:" + redirectURL;

        } catch (LoginException e) {
            model.addAttribute("errorType", e.getErrorType());
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("redirectURL", redirectURL);
            return "users/login";
        }
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new SignupRequest());
        return "users/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("user") SignupRequest signupRequest,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            return "users/signup";
        }

        try {
            userService.signup(signupRequest);
            log.info("회원가입 성공: username={}", signupRequest.getUsername());
            return redirectToLogin();

        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        expireAccessTokenCookie(response);
        log.info("로그아웃 완료");
        return redirectToLogin();
    }

    @GetMapping("/me")
    public String myPage(@LoginUser User user, Model model) {
        if (user == null) {
            return redirectToLogin();
        }
        model.addAttribute("user", user);
        return "users/me";
    }

    @GetMapping("/update")
    public String updateForm(@LoginUser User user, Model model) {
        if (user == null) {
            return redirectToLogin();
        }
        UserUpdateFormRequest formRequest = new UserUpdateFormRequest();
        formRequest.setNickname(user.getNickname());
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
                         Model model) {
        if (user == null) {
            return redirectToLogin();
        }

        if (bindingResult.hasErrors()) {
            return "users/update";
        }

        try {
            userService.updateUserInfo(user.getId(), formRequest);
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
            return redirectToLogin();
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
            return redirectToLogin();
        }

        if (bindingResult.hasErrors()) {
            return "users/delete";
        }

        try {
            userService.deleteUser(user.getId(), withdrawRequest.getPassword());
            expireAccessTokenCookie(response);
            log.info("회원 탈퇴 완료: userId={}", user.getId());
            return redirectToLogin();
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
            return redirectToLogin();
        }

        if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordMismatch",
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeRequest", passwordRequest);
            model.addAttribute("passwordChangeError", true);
            return "users/update";
        }

        try {
            userService.changePassword(user.getId(),
                    passwordRequest.getCurrentPassword(),
                    passwordRequest.getNewPassword());
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
            String username = userService.findUsernameByEmail(email);
            model.addAttribute("successMessage", "회원님의 아이디는 [" + username + "] 입니다.");
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
    public String resetPassword(@RequestParam("username") String username,
                                @RequestParam("email") String email,
                                Model model) {
        try {
            userService.resetPassword(username, email);
            model.addAttribute("successMessage", "임시 비밀번호가 이메일로 발송되었습니다.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "users/reset-pw";
    }

    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(ACCESS_TOKEN_MAX_AGE);
        response.addCookie(cookie);
    }

    private void expireAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String redirectToLogin() {
        return "redirect:" + loginUrl;
    }
}
