package com.pagerealm.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Response to client端：User Profile頁面中顯示的資訊
 */
@Getter
@Setter
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String gender;// added 8/14
    private LocalDate birthdate; // added 8/14
    private String membershipTier; // added 8/14
    private String avatarUrl; // added 8/14
    private boolean enabled;
    private boolean totpEnabled;
    private List<String> roles;

    public UserInfoResponse(Long id, String username,
                            String email,
                            String gender,
                            LocalDate birthdate,
                            String membershipTier,
                            String avatarUrl,
                            boolean enabled,
                            boolean totpEnabled,
                            List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.birthdate = birthdate;
        this.membershipTier = membershipTier;
        this.avatarUrl = avatarUrl;
        this.enabled = enabled;
        this.totpEnabled = totpEnabled;
        this.roles = roles;
    }
}
