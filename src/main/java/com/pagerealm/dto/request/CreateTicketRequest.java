package com.pagerealm.dto.request;

import com.pagerealm.entity.AppTicketCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {
    @NotBlank
    private String contactName;

    @NotBlank
    @Email
    private String contactEmail;

    @NotNull
    private AppTicketCategory category;

    @NotBlank
    private String subject;

    @NotBlank
    private String content;
}
