package com.spring.blog.user.presentation;

import static com.spring.blog.common.ApiDocumentUtils.getDocumentRequest;
import static com.spring.blog.common.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.blog.authentication.application.AuthService;
import com.spring.blog.authentication.domain.user.LoginUser;
import com.spring.blog.user.application.UserService;
import com.spring.blog.user.application.dto.UserRegistrationRequestDto;
import com.spring.blog.user.presentation.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("UserController 슬라이스 테스트")
@AutoConfigureRestDocs
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @DisplayName("비로그인 유저는 회원 탈퇴를 할 수 없다.")
    @Test
    void withdraw_GuestUser_Failure() throws Exception {
        // given
        given(authService.validateToken(any())).willReturn(false);

        // when, then
        ResultActions resultActions = mockMvc.perform(delete("/api/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("A0001"));

        verify(authService, times(1)).validateToken(any());

        // restDocs
        resultActions.andDo(document("withdraw-not-login",
            getDocumentRequest(),
            getDocumentResponse(),
            responseFields(fieldWithPath("errorCode").description("권한 예외")))
        );
    }

    @DisplayName("로그인 유저는 회원 탈퇴를 할 수 있다.")
    @Test
    void withdraw_LoginUser_Success() throws Exception {
        // given
        given(authService.validateToken("token")).willReturn(true);
        given(authService.findRequestUserByToken("token"))
            .willReturn(new LoginUser(1L, "kevin"));
        Mockito.doNothing().when(userService).withdraw(1L);

        // when, then
        ResultActions resultActions = mockMvc.perform(delete("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
            .andExpect(status().isNoContent());

        verify(authService, times(1)).validateToken("token");
        verify(authService, times(1)).findRequestUserByToken("token");
        verify(userService, times(1)).withdraw(1L);

        // restDocs
        resultActions.andDo(document("withdraw-login",
            getDocumentRequest(),
            getDocumentResponse(),
            requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer token")))
        );
    }

    @DisplayName("OAuth 기반으로 회원가입할 수 있다.")
    @Test
    void registerByOauth_Success() throws Exception {
        // given
        UserRegistrationRequest userRegistrationRequest = UserRegistrationRequest.builder()
            .name("abc")
            .email("abc@naver.com")
            .build();
        doNothing().when(userService).registerByOauth(any(UserRegistrationRequestDto.class));

        // when, then
        ResultActions resultActions = mockMvc.perform(post("/api/users/oauth")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userRegistrationRequest)))
            .andExpect(status().isCreated());

        // restDocs
        resultActions.andDo(document("register-oauth",
            getDocumentRequest(),
            getDocumentResponse())
        );
    }
}
