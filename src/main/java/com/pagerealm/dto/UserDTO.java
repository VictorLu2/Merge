package com.pagerealm.dto;

import com.pagerealm.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

//實務上不應該把User password傳給前端，所以UserDTO包含所有User的欄位 除了"password"
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
    private Long userId;
    private String userName;
    private String email;
    private String gender;
    private LocalDate birthdate;
    private String membershipTier;
    private String avatarUrl;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private boolean enabled;
    private boolean totpEnabled;
    private String totpSecret;
    private String signedMethod;
    private Role role;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
