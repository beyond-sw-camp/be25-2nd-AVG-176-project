package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.PasswordChangeRequest;
import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.SessionUser;
import com.example.team3Project.domain.user.dto.SignupRequest;
import com.example.team3Project.domain.user.dto.UserUpdateFormRequest;
import com.example.team3Project.domain.user.dto.UserWithdrawRequest;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.exception.LoginException;
import com.example.team3Project.global.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserService userService;

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
                        HttpServletRequest request,
                        Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("redirectURL", redirectURL);
            return "users/login";
        }

        try {
            User loginUser = userService.login(loginRequest);
            SessionUtils.setLoginUser(
                    request,
                    new SessionUser(
                            loginUser.getId(),
                            loginUser.getUsername(),
                            loginUser.getNickname()
                    )
            );

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
            return "redirect:/users/login";

        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        SessionUtils.invalidateSession(request);
        log.info("로그아웃 완료");
        return "redirect:/users/login";
    }

    @GetMapping("/me")
    public String myPage(@LoginUser User user, Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("user", user);
        return "users/me";
    }

    @GetMapping("/update")
    public String updateForm(@LoginUser User user, Model model) {
        if (user == null) {
            return "redirect:/users/login";
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
            return "redirect:/users/login";
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
            return "redirect:/users/login";
        }
        model.addAttribute("withdrawRequest", new UserWithdrawRequest());
        return "users/delete";
    }

    @PostMapping("/delete")
    public String delete(@LoginUser User user,
                         @Valid @ModelAttribute("withdrawRequest") UserWithdrawRequest withdrawRequest,
                         BindingResult bindingResult,
                         HttpServletRequest request,
                         Model model) {
        if (user == null) {
            return "redirect:/users/login";
        }

        if (bindingResult.hasErrors()) {
            return "users/delete";
        }

        try {
            userService.deleteUser(user.getId(), withdrawRequest.getPassword());
            SessionUtils.invalidateSession(request);
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
}