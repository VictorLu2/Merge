package com.pagerealm.security.jwt;

import com.pagerealm.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    //logger
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);


    /**
     * @description :
     * - 取得並驗證 client 端 request 中的 JWT token
     * - 從 token 解析出 username
     * - 以 username 載入對應的 UserDetails
     * - 用 `UsernamePasswordAuthenticationToken` 建立認證物件
     * - 將認證資訊設置到 Spring Security 的安全上下文中，完成自定義 filter 的認證流程
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());

        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 若為 2FA 暫時性 Token，不建立認證（只放行給 2FA 驗證端點使用）
                if (jwtUtils.is2FAToken(jwt)) {
                    logger.debug("2FA temp token detected, skipping SecurityContext authentication.");
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = jwtUtils.getUserEmailFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                logger.debug("Roles from JWT: {}", userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * @description : 解析並取得request header中的JWT Token
     * @param request
     * @return : JWT Token
     */
    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        logger.debug("AuthTokenFilter.java: {}", jwt);
        return jwt;
    }
}
