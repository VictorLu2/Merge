package com.pagerealm.security.config;

import com.pagerealm.config.OAuth2LoginSuccessHandler;
import com.pagerealm.entity.AppRole;
import com.pagerealm.entity.Role;
import com.pagerealm.repository.RoleRepository;
import com.pagerealm.security.jwt.AuthEntryPointJwt;
import com.pagerealm.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)//方法級別權限控制
public class SecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    @Lazy //延遲初始化：讓spring在需要用到時才建立bean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

//        http.csrf(csrf ->
//                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .ignoringRequestMatchers("/api/auth/public/**")
//        );
        http.cors(Customizer.withDefaults());

        http.authorizeHttpRequests(request -> request
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        //.requestMatchers("/api/csrf-token").permitAll()
                        .requestMatchers("/api/auth/public/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> {
                    oauth2.successHandler(oAuth2LoginSuccessHandler);
                })
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler))
                .addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //http.formLogin(Customizer.withDefaults());
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_USER)));

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_ADMIN)));
        };
    }
}
