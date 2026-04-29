package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.BlockedWordService;
import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockedWordControllerTest {

    private MockMvc mockMvc;
    private BlockedWordService blockedWordService;

    @BeforeEach
    void setUp() {
        blockedWordService = mock(BlockedWordService.class);
        BlockedWordController controller = new BlockedWordController(blockedWordService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(createUser(1L, "tester", "테스터")))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("금지어 등록 API는 로그인 사용자 기준으로 금지어를 생성한다")
    void createBlockedWord_usesLoginUser() throws Exception {
        when(blockedWordService.createBlockedWord(any(), any()))
                .thenReturn(new BlockedWordResponse(1L, 1L, "금지어"));

        mockMvc.perform(
                        post("/policies/blocked-words")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "blockedWord": "금지어"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userBlockedWordId").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.blockedWord").value("금지어"));

        verify(blockedWordService).createBlockedWord(eq(1L), any());
    }

    @Test
    @DisplayName("금지어 목록 조회 API는 로그인 사용자 기준으로 목록을 반환한다")
    void getBlockedWords_returnsBlockedWordList() throws Exception {
        when(blockedWordService.getBlockedWords(1L)).thenReturn(List.of(
                new BlockedWordResponse(1L, 1L, "금지어1"),
                new BlockedWordResponse(2L, 1L, "금지어2")
        ));

        mockMvc.perform(get("/policies/blocked-words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].blockedWord").value("금지어1"))
                .andExpect(jsonPath("$[1].blockedWord").value("금지어2"));

        verify(blockedWordService).getBlockedWords(1L);
    }

    @Test
    @DisplayName("금지어 수정 API는 로그인 사용자 기준으로 수정한다")
    void updateBlockedWord_usesLoginUser() throws Exception {
        when(blockedWordService.updateBlockedWord(any(), any(), any()))
                .thenReturn(new BlockedWordResponse(3L, 1L, "수정금지어"));

        mockMvc.perform(
                        put("/policies/blocked-words/3")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "blockedWord": "수정금지어"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userBlockedWordId").value(3))
                .andExpect(jsonPath("$.blockedWord").value("수정금지어"));

        verify(blockedWordService).updateBlockedWord(eq(1L), eq(3L), any());
    }

    @Test
    @DisplayName("금지어 삭제 API는 로그인 사용자 기준으로 삭제한다")
    void deleteBlockedWord_usesLoginUser() throws Exception {
        mockMvc.perform(delete("/policies/blocked-words/5"))
                .andExpect(status().isNoContent());

        verify(blockedWordService).deleteBlockedWord(1L, 5L);
    }

    @Test
    @DisplayName("소싱 금지어 조회 API는 문자열 목록만 반환한다")
    void getBlockedWordsForSourcing_returnsBlockedWordStrings() throws Exception {
        when(blockedWordService.getBlockedWords(1L)).thenReturn(List.of(
                new BlockedWordResponse(1L, 1L, "금지어1"),
                new BlockedWordResponse(2L, 1L, "금지어2")
        ));

        mockMvc.perform(get("/policies/blocked-words/sourcing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("금지어1"))
                .andExpect(jsonPath("$[1]").value("금지어2"));
    }

    @Test
    @DisplayName("금지어 API는 로그인 사용자가 없으면 401을 반환한다")
    void blockedWordApis_returnUnauthorized_whenNoLoginUser() throws Exception {
        BlockedWordController controller = new BlockedWordController(blockedWordService);
        MockMvc unauthorizedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(null))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        unauthorizedMockMvc.perform(get("/policies/blocked-words"))
                .andExpect(status().isUnauthorized());

        unauthorizedMockMvc.perform(
                        post("/policies/blocked-words")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "blockedWord": "금지어"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(blockedWordService);
    }

    private User createUser(Long id, String username, String nickname) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", username);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        return user;
    }

    private static class LoginUserTestArgumentResolver implements HandlerMethodArgumentResolver {

        private final User user;

        private LoginUserTestArgumentResolver(User user) {
            this.user = user;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(LoginUser.class)
                    && User.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return user;
        }
    }
}
