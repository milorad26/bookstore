package com.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request for forgot password")
public class ForgotPasswordRequest {
    
    @NotBlank(message = "{auth.password.reset.username.empty}")
    @Schema(description = "Username of the account", example = "john_doe", required = true)
    private String username;
}
