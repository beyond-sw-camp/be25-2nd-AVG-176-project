package com.example.team3Project.domain.home;

import com.example.team3Project.domain.user.dto.SessionUser;
import com.example.team3Project.global.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        SessionUser loginUser = SessionUtils.getLoginUser(request);
        
        if (loginUser == null) {
            return "home";
        }
        
        model.addAttribute("user", loginUser);
        return "loginHome";
    }
}
