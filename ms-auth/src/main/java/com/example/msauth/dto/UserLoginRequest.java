package com.example.msauth.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String email;
    private String password;
}
