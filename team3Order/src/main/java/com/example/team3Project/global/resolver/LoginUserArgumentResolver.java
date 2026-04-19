package com.example.team3Project.global.resolver;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Long userId = parseUserId(webRequest.getHeader(USER_ID_HEADER));
        return userId == null ? null : new User(userId);
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        String firstValue = userIdHeader.split(",")[0].trim();
        if (firstValue.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(firstValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
