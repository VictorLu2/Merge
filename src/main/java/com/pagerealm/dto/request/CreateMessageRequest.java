package com.pagerealm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank
    private String content;
}

