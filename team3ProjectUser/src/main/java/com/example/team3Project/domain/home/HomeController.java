package com.example.team3Project.domain.home;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserService;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        // JWT 토큰에서 사용자 정보 확인
        String token = jwtUtil.resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            String nickname = jwtUtil.getNickname(token);

            if (userId != null) {
                // DB에서 최신 사용자 정보 조회 (또는 토큰 정보 직접 사용)
                User user = userService.findById(userId).orElse(null);
                if (user != null) {
                    model.addAttribute("user", user);
                    return "loginHome";
                }
            }
        }

        // 인증되지 않은 사용자는 메인 페이지로
        return "home";
    }
}
