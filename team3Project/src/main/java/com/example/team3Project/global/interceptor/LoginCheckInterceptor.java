package com.example.team3Project.global.interceptor;

import com.example.team3Project.global.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        log.info("인증 체크 인터셉터 실행 {}", requestURI);
        
        if (!SessionUtils.isLoggedIn(request)) {
            log.info("미인증 사용자 요청");
            response.sendRedirect("/users/login?redirectURL=" + requestURI);
            return false;
        }
        
        return true;
    }
}
