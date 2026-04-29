package com.example.team3Project.global.resolver;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserService;
import com.example.team3Project.global.annotation.LoginUser;
import com.example.team3Project.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    // @LoginUser 파라미터가 붙은 컨트롤러 메서드에서 JWT를 해석해 User 엔티티를 주입한다.
    // 토큰이 없거나 무효하면 null을 반환하고, 각 컨트롤러가 401/비로그인 처리 분기를 담당한다.

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginUserAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean isUserType = User.class.isAssignableFrom(parameter.getParameterType());
        return hasLoginUserAnnotation && isUserType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // JWT 토큰 추출 (Cookie 또는 Header)
        String token = jwtUtil.resolveToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            log.debug("JWT 토큰이 없거나 유효하지 않습니다");
            return null;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return null;
        }

        return userService.findById(userId).orElse(null);
    }
}
