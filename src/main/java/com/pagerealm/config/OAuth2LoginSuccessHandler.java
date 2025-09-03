package com.pagerealm.config;

import com.pagerealm.entity.AppRole;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.entity.Role;
import com.pagerealm.entity.User;
import com.pagerealm.repository.RoleRepository;
import com.pagerealm.security.jwt.JwtUtils;
import com.pagerealm.security.service.UserDetailsImpl;
import com.pagerealm.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor//產生一個包含所有final屬性的建構式
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    String username;
    String idAttributeKey;

    //-------------------------------------

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;

        String provider = oAuth2Token.getAuthorizedClientRegistrationId(); // github | google | facebook

        if (!"github".equals(provider) && !"google".equals(provider)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String email = String.valueOf(attributes.getOrDefault("email", ""));
        String name = String.valueOf(attributes.getOrDefault("name", ""));

        if ("github".equals(provider)) {
            username = String.valueOf(attributes.getOrDefault("login", ""));
            idAttributeKey = "id";
        } else { //google
            username = email != null && email.contains("@") ? email.split("@")[0] : "";
            idAttributeKey = "sub";
        }

        userService.findByEmail(email).ifPresentOrElse(user -> {
            DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                    attributes,
                    idAttributeKey
            );

            Authentication securityAuth = new OAuth2AuthenticationToken(
                    oAuth2User,
                    List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                    provider
            );
            SecurityContextHolder.getContext().setAuthentication(securityAuth);
        }, () -> {
            User newUser = new User();
            Optional<Role> userRole = roleRepository.findByRoleName(AppRole.ROLE_USER);
            if (userRole.isEmpty()) {
                throw new RuntimeException("Default role not found");
            }
            newUser.setEnabled(true);
            newUser.setMembershipTier(MembershipTier.LV1);
            newUser.setRole(userRole.get());
            newUser.setEmail(email);
            newUser.setUserName(username);
            newUser.setSignedMethod(provider);
            userService.registerOAuth2User(newUser);

            DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                    attributes,
                    idAttributeKey
            );

            Authentication securityAuth = new OAuth2AuthenticationToken(
                    oAuth2User,
                    List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                    provider
            );
            SecurityContextHolder.getContext().setAuthentication(securityAuth);
        });

        this.setAlwaysUseDefaultTargetUrl(true);

        // 重新取得使用者資料與權限
        User user = userService.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Set<SimpleGrantedAuthority> authorities = new HashSet<>(principal.getAuthorities().stream()
                .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                .collect(Collectors.toList()));
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName().name()));

        UserDetailsImpl userDetails = new UserDetailsImpl(
                null,
                username,
                email,
                null,
                user.isTotpEnabled(), // 將 TOTP 狀態帶入
                user.isEnabled(),
                authorities
        );

        String targetUrl;
        if (user.isTotpEnabled()) {
            // 啟用 TOTP：回傳 require2fa 與 tempJwt
            String tempJwt = jwtUtils.generateTempTokenFor2FA(userDetails);
            targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("require2fa", true)
                    .queryParam("tempJwt", tempJwt)
                    .build().toUriString();
        } else {
            // 未啟用 TOTP：直接回傳正式 JWT
            String jwtToken = jwtUtils.generateTokenFromUserEmail(userDetails);
            targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", jwtToken)
                    .build().toUriString();
        }

        this.setDefaultTargetUrl(targetUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}