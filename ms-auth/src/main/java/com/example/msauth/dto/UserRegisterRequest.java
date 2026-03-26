package com.example.msauth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {
    private @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email;

    private @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50,message = "First name must be between 2 and 50 characters")
    String firstName;

    private @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50,message = "Last name must be between 2 and 50 characters")
    String lastName;

    private @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password;
    private String role;
}
