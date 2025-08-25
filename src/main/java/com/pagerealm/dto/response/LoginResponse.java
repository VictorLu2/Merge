package com.pagerealm.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class LoginResponse {
    private String jwtToken;
    private String email; // username -> email (8/14)
    private List<String> roles;

    public LoginResponse(String email, List<String> roles, String jwtToken) {
        this.email = email;
        this.roles = roles;
        this.jwtToken = jwtToken;
    }
}
