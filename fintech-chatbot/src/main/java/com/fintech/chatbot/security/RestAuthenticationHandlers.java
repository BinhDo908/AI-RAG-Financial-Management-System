package com.fintech.chatbot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Trả lỗi 401/403 dạng JSON {@link ApiResponse} thay vì trang lỗi mặc định.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationHandlers {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> write(response, ErrorCode.UNAUTHORIZED);
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(code.name(), code.getDefaultMessage(), null));
    }
}
