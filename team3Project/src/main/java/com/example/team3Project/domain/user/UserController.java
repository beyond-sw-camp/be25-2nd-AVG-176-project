package com.example.team3Project.domain.user;

import com.example.team3Project.domain.user.dto.LoginRequest;
import com.example.team3Project.domain.user.dto.SessionUser;
import com.example.team3Project.domain.user.dto.SignupRequest;
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
            return "users/login";
        }

        try {
            User loginUser = userService.login(loginRequest);
            SessionUtils.setLoginUser(request, new SessionUser(loginUser.getId(), loginUser.getUsername()));

            log.info("로그인 성공: userId={}, redirectURL={}", loginUser.getId(), redirectURL);
            return "redirect:" + redirectURL;

        } catch (LoginException e) {
            model.addAttribute("errorType", e.getErrorType());
            model.addAttribute("loginRequest", loginRequest);
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
        return "redirect:/";
    }
}
