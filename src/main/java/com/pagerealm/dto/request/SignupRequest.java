package com.pagerealm.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.Set;

/**
 * @Valid註解
 * 1. @NotBlank : 不可為null, 也不可為空字串 " "
 * 2. @NotNull : 不可為null, 但可以是空字串
 */

@Data
public class SignupRequest {

    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    //保留擴充性，普通用戶無法自行選擇角色，但管理者可能可以
    private Set<String> role;

    @NotBlank
    @Size(min = 6,max = 40)
    private String password;

    @NotBlank
    private String gender;

    @NotNull
    @Past
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;


}
