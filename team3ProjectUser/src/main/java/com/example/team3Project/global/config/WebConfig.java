package com.example.team3Project.global.config;

import com.example.team3Project.global.interceptor.JwtCheckInterceptor;
import com.example.team3Project.global.resolver.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtCheckInterceptor jwtCheckInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Value("${app.gateway-url:http://100.119.201.17:9000}")
    private String gatewayUrl;

    @Value("${app.frontend-url:http://100.119.201.17:9000}")
    private String frontendUrl;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtCheckInterceptor)
                .addPathPatterns("/users/me", "/users/update", "/users/delete")
                .excludePathPatterns("/users/login", "/users/signup", "/users/find-id", "/users/reset-pw");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // API Gateway 기반 구조 - 외부 클라이언트는 반드시 Gateway를 통해 접근
        registry.addMapping("/**")
                .allowedOrigins(gatewayUrl, frontendUrl)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
