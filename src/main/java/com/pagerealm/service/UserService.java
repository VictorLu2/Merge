package com.pagerealm.service;

import com.pagerealm.dto.UserDTO;
import com.pagerealm.dto.request.EmailVerifyRequest;
import com.pagerealm.entity.Role;
import com.pagerealm.entity.User;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> getAllUsers();

    UserDTO getUserById(Long id);

    User findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);

    void updateAccountEnabledStatus(Long userId, boolean enabled);

    void updatePassword(Long userId, String password, String checkPassword);

    List<Role> getAllRoles();

    void generatePasswordResetToken(String email);

    void resetPassword(String token, String newPassword, String checkPassword);

    void buildAndSendVerificationEmail(String rawCode, String email);

    void verifyUser(EmailVerifyRequest request);

    void resendVerificationCode(String email);

    //產生隨機6位數驗證碼
    String generateVerificationCode();

    User registerOAuth2User(User newUser);

    GoogleAuthenticatorKey generateTotpSecret(Long userId);

    boolean validateTotpCode(Long userId, int code);

    void enableTotp(Long userId);

    void disableTotp(Long userId);

    void updateBirthdate(Long userId, LocalDate birthdate);

    void updateGender(Long userId, String gender);

    void updateUsername(Long userId, String username);

    String uploadAvatar(Long userId, MultipartFile file);
}
