package com.example.team3Project.domain.home;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Value("${app.auth.login-url:http://100.119.201.17:9000/users/login}")
    private String loginUrl;

    @Value("${app.auth.signup-url:http://100.119.201.17:9000/users/signup}")
    private String signupUrl;

    @GetMapping("/")
    public String home(@LoginUser User user, Model model) {
        if (user == null) {
            model.addAttribute("loginUrl", loginUrl);
            model.addAttribute("signupUrl", signupUrl);
            return "home";
        }

        model.addAttribute("user", user);
        return "loginHome";
    }
}
