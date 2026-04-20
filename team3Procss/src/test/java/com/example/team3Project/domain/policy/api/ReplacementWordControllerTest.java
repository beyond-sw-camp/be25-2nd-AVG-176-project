package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.ReplacementWordService;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

class ReplacementWordControllerTest {

    private MockMvc mockMvc;
    private ReplacementWordService replacementWordService;

    @BeforeEach
    void setUp() {
        replacementWordService = mock(ReplacementWordService.class);
        ReplacementWordController controller = new ReplacementWordController(replacementWordService);

        // 치환어 컨트롤러가 로그인 사용자 기준으로 서비스 호출하는지만 확인한다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(createUser(1L)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("치환어 등록 API는 로그인 사용자 기준으로 생성한다")
    void createReplacementWord_usesLoginUser() throws Exception {
        when(replacementWordService.createReplacementWord(any(), any()))
                .thenReturn(new ReplacementWordResponse(1L, 1L, "원본", "치환"));

        mockMvc.perform(
                        post("/policies/replacement-words")
                                .contentType("application/json")
                                .content(validCreateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReplacementWordId").value(1))
                .andExpect(jsonPath("$.sourceWord").value("원본"))
                .andExpect(jsonPath("$.replacementWord").value("치환"));

        verify(replacementWordService).createReplacementWord(eq(1L), any());
    }

    @Test
    @DisplayName("치환어 목록 조회 API는 로그인 사용자 기준으로 조회한다")
    void getReplacementWords_usesLoginUser() throws Exception {
        when(replacementWordService.getReplacementWords(1L)).thenReturn(List.of(
                new ReplacementWordResponse(1L, 1L, "원본1", "치환1"),
                new ReplacementWordResponse(2L, 1L, "원본2", "치환2")
        ));

        mockMvc.perform(get("/policies/replacement-words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sourceWord").value("원본1"))
                .andExpect(jsonPath("$[1].replacementWord").value("치환2"));

        verify(replacementWordService).getReplacementWords(1L);
    }

    @Test
    @DisplayName("치환어 수정 API는 로그인 사용자 기준으로 수정한다")
    void updateReplacementWord_usesLoginUser() throws Exception {
        when(replacementWordService.updateReplacementWord(any(), any(), any()))
                .thenReturn(new ReplacementWordResponse(3L, 1L, "수정원본", "수정치환"));

        mockMvc.perform(
                        put("/policies/replacement-words/3")
                                .contentType("application/json")
                                .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userReplacementWordId").value(3))
                .andExpect(jsonPath("$.sourceWord").value("수정원본"))
                .andExpect(jsonPath("$.replacementWord").value("수정치환"));

        verify(replacementWordService).updateReplacementWord(eq(1L), eq(3L), any());
    }

    @Test
    @DisplayName("치환어 삭제 API는 로그인 사용자 기준으로 삭제한다")
    void deleteReplacementWord_usesLoginUser() throws Exception {
        mockMvc.perform(delete("/policies/replacement-words/4"))
                .andExpect(status().isNoContent());

        verify(replacementWordService).deleteReplacementWord(1L, 4L);
    }

    @Test
    @DisplayName("치환어 API는 로그인 사용자가 없으면 401을 반환한다")
    void replacementWordApis_returnUnauthorized_whenNoLoginUser() throws Exception {
        ReplacementWordController controller = new ReplacementWordController(replacementWordService);
        MockMvc unauthorizedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserTestArgumentResolver(null))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        unauthorizedMockMvc.perform(get("/policies/replacement-words"))
                .andExpect(status().isUnauthorized());

        unauthorizedMockMvc.perform(
                        post("/policies/replacement-words")
                                .contentType("application/json")
                                .content(validCreateRequest()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(replacementWordService);
    }

    private String validCreateRequest() {
        return """
                {
                  "sourceWord": "원본",
                  "replacementWord": "치환"
                }
                """;
    }

    private String validUpdateRequest() {
        return """
                {
                  "sourceWord": "수정원본",
                  "replacementWord": "수정치환"
                }
                """;
    }

    private User createUser(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", "tester");
        ReflectionTestUtils.setField(user, "nickname", "테스터");
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
            // 실제 ArgumentResolver 대신 테스트용 로그인 사용자를 그대로 주입한다.
            return user;
        }
    }
}
