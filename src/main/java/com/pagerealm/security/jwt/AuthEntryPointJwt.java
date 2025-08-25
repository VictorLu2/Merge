package com.pagerealm.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    /**
     * @description : 此方法負責在未授權存取時，回傳結構化的JSON錯誤訊息與401狀態碼，並記錄相關日誌
     * 提升API安全性與可維護性
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        logger.error("Unauthorized error: {}", authException.getMessage());
        System.out.println(authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);//將response格式設為application/json
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//將response status code設為401(Unauthorized)

        //建立一個Map結構並放入 狀態碼、錯誤類型、錯誤訊息、request的請求路徑，建構出Response body
        final Map<String,Object> body = new HashMap<>();
        body.put("status",HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error","Unauthorized");
        body.put("message",authException.getMessage());
        body.put("path",request.getServletPath());

        //Jackson ObjectMapper物件，用來將Java物件轉為JSON格式
        final ObjectMapper mapper = new ObjectMapper();
        //將body這個map結構的物件，序列化為JSON，並直接寫入HTTP response的ops，讓前端收到JSON格式的錯誤訊息
        mapper.writeValue(response.getOutputStream(),body);
    }
}
