package com.example.team3Project.global.security.oauth2;

import com.example.team3Project.domain.user.User;
import com.example.team3Project.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info("OAuth2 로그인 시도 - Provider: {}, UserNameAttribute: {}", registrationId, userNameAttributeName);

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oauth2User.getAttributes());

        User.AuthProvider authProvider = User.AuthProvider.valueOf(registrationId.toUpperCase());
        User user = saveOrUpdate(attributes, authProvider);

        return new CustomOAuth2User(user, oauth2User.getAttributes(), userNameAttributeName);
    }

    private User saveOrUpdate(OAuthAttributes attributes, User.AuthProvider provider) {
        String providerId = attributes.getProviderId();
        String email = attributes.getEmail();

        // provider_id로 먼저 조회
        Optional<User> userOptional = userRepository.findByProviderId(providerId);

        if (userOptional.isPresent()) {
            // 기존 소셜 회원 - 정보 업데이트
            User user = userOptional.get();
            user.setName(attributes.getName());
            user.setNickname(attributes.getName());
            user.setProfileImage(attributes.getPicture());
            log.info("기존 소셜 회원 로그인: {}", user.getUsername());
            return user;
        }

        // 이메일로 조회 (일반 회원가입된 이메일과 연동)
        if (email != null && !email.isEmpty()) {
            Optional<User> emailUserOptional = userRepository.findByEmail(email);
            if (emailUserOptional.isPresent()) {
                User user = emailUserOptional.get();
                // 소셜 계정 연동
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setSocialLinked(true);
                user.setProfileImage(attributes.getPicture());
                log.info("기존 일반 회원에 소셜 계정 연동: {}", email);
                return user;
            }
        }

        // 새로운 소셜 회원가입
        User newUser = attributes.toEntity(provider);
        User savedUser = userRepository.save(newUser);
        log.info("새로운 소셜 회원가입 완료: {}", savedUser.getUsername());
        return savedUser;
    }
}
