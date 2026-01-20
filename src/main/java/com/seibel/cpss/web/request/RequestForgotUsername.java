package com.seibel.cpss.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RequestForgotUsername {
    @NotEmpty(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
