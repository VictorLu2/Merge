package com.pagerealm.controller;

import com.pagerealm.dto.request.EmailVerifyRequest;
import com.pagerealm.dto.request.LoginRequest;
import com.pagerealm.dto.request.SignupRequest;
import com.pagerealm.dto.response.LoginResponse;
import com.pagerealm.dto.response.MessageResponse;
import com.pagerealm.dto.response.UserInfoResponse;
import com.pagerealm.entity.AppRole;
import com.pagerealm.entity.MembershipTier;
import com.pagerealm.entity.Role;
import com.pagerealm.entity.User;
import com.pagerealm.repository.RoleRepository;
import com.pagerealm.repository.UserRepository;
import com.pagerealm.security.jwt.JwtUtils;
import com.pagerealm.security.service.UserDetailsImpl;
import com.pagerealm.service.TotpService;
import com.pagerealm.service.UserService;
import com.pagerealm.utils.AuthUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.validation.Valid;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//Controller級別CORS配置
//@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    //log
    private Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthUtil authUtil;
    private final TotpService totpService;
    private JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    public AuthController(JwtUtils jwtUtils, AuthenticationManager authenticationManager, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserService userService, AuthUtil authUtil, TotpService totpService) {
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.authUtil = authUtil;
        this.totpService = totpService;
    }


    //-----Signin, Signup (logout前端負責)-----
    @PostMapping("/public/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication;

        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        } catch (DisabledException e) {
            //帳號若未啟用
            Map<String, Object> map = new HashMap<>();
            map.put("message", "此帳號尚未啟用，請完成email認證");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.FORBIDDEN);

        } catch (AuthenticationException e) {
            //Credentials輸入錯誤
            Map<String, Object> map = new HashMap<>();
            map.put("message", "輸入的email或密碼錯誤，請重新輸入");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
        }
        //取得當前登入的User資訊
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userService.findByEmail(userDetails.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));

        //檢查雙重驗證是否啟用
        if (user.isTotpEnabled()) {
            String tempJwt = jwtUtils.generateTempTokenFor2FA(userDetails);
            Map<String, Object> map = new HashMap<>();
            map.put("require2fa", true);
            map.put("tempJwt", tempJwt);
            return ResponseEntity.ok(map);
        }

        //將目前的Authentication資訊存入Spring Security的安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        //使用當前user的email作為subject，建構出要放進response傳給前端的jwtToken
        String jwtToken = jwtUtils.generateTokenFromUserEmail(userDetails);

        //取得當前User的所有權限名稱，並轉成List<String>
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        //建立要返回給前端的response物件，並放入對應的資料 (email, roles, jwtToken)
        LoginResponse response = new LoginResponse(userDetails.getUsername(), roles, jwtToken);

        //回傳response給前端(with 200 status code)
        return ResponseEntity.ok(response);
    }


    @PostMapping("/public/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {

        //判斷client端輸入的username和email是否已存在
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: 此使用者名稱已有人使用"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: 此Email已註冊過"));
        }

        //創建新的使用者帳號
        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()),
                signupRequest.getGender(),
                signupRequest.getBirthdate()
        );
        String rawCode = userService.generateVerificationCode();
        String hashCode = DigestUtils.sha256Hex(rawCode);

        userService.buildAndSendVerificationEmail(rawCode, user.getEmail());

        user.setEnabled(false);
        user.setMembershipTier(MembershipTier.LV1);
        user.setTotpEnabled(false);
        user.setSignedMethod("email");
        user.setAvatarUrl("/images/Avatar_default.png");
        user.setVerificationCode(hashCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        // 角色判斷：user | admin
        Set<String> strRoles = signupRequest.getRole();

        String roleStr = (strRoles != null && !strRoles.isEmpty()) ? strRoles.iterator().next() : "user";

        AppRole appRole = "admin".equals(roleStr) ? AppRole.ROLE_ADMIN : AppRole.ROLE_USER;

        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new RuntimeException("Error: Role not found"));

        user.setRole(role);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("請至信箱完成驗證"));
    }


    //-----Email Verification-----
    @PostMapping("/public/verify")
    public ResponseEntity<?> verifyUser(@RequestBody EmailVerifyRequest request) {

        try {
            userService.verifyUser(request);
            return ResponseEntity.ok("Email驗證成功！");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/public/resend-verification")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {

        try {
            userService.resendVerificationCode(email);
            return ResponseEntity.ok("驗證信已寄出");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //-----Forgot Password----
    @PostMapping("/public/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {

        try {
            userService.generatePasswordResetToken(email);
            return ResponseEntity.ok(new MessageResponse("密碼重置信件已寄至您的信箱"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("驗證信寄送失敗[Debug用]"));
        }
    }

    @PostMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword, @RequestParam String checkPassword) {
        try {
            userService.resetPassword(token, newPassword, checkPassword);
            return ResponseEntity.ok(new MessageResponse("密碼重設成功！"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    //----2FA Authentication (TOTP)------
    @PostMapping("/enable-totp")
    public ResponseEntity<String> enableTotp() {
        Long userId = authUtil.LoggedInUserId();

        GoogleAuthenticatorKey secret = userService.generateTotpSecret(userId);

        //可優化
        String qrCodeUrl = totpService.getQrCodeUrl(secret, userService.getUserById(userId).getEmail());

        return ResponseEntity.ok(qrCodeUrl);
    }

    @PostMapping("/disable-totp")
    public ResponseEntity<String> disableTotp() {
        Long userId = authUtil.LoggedInUserId();
        userService.disableTotp(userId);
        return ResponseEntity.ok("雙重認證已停用");
    }

    @PostMapping("/verify-totp")
    public ResponseEntity<String> verifyTotp(@RequestParam int code) {

        Long userId = authUtil.LoggedInUserId();
        boolean isValid = userService.validateTotpCode(userId, code);

        if (isValid) {
            userService.enableTotp(userId);
            return ResponseEntity.ok("2FA code Verified");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid 2FA code");
        }
    }

    @GetMapping("/user/totp-status")
    public ResponseEntity<?> getTotpStatus() {
        User user = authUtil.loggedInUser();
        if (user != null) {
            return ResponseEntity.ok().body(Map.of("is2faEnabled", user.isTotpEnabled()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PostMapping("/public/verify-totp-login")
    public ResponseEntity<?> verifyTotpLogin(
            @RequestParam int code,
            @RequestParam String tempJwt) {

        if (!jwtUtils.is2FAToken(tempJwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid 2FA token");
        }
        String email = jwtUtils.getUserEmailFromJwtToken(tempJwt);
        User user = userService.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        boolean isValid = userService.validateTotpCode(user.getUserId(), code);
        if (isValid) {
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            String jwtToken = jwtUtils.generateTokenFromUserEmail(userDetails);
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());
            LoginResponse response = new LoginResponse(userDetails.getUsername(), roles, jwtToken);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid 2FA Code");
        }

    }

}
