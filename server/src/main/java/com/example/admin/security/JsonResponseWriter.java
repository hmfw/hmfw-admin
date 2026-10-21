package com.example.admin.security;

import com.example.admin.common.api.R;
import com.example.admin.common.api.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 401 / 403 共用的 JSON 输出，保证过滤器链抛出的响应体与 Controller 一致 */
@Component
@RequiredArgsConstructor
public class JsonResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int httpStatus, ResultCode resultCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), R.fail(resultCode));
    }
}
