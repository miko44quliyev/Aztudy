package com.example.msauth.dto;

import com.example.msauth.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    private Boolean active;
    private LocalDateTime createdAt;

    private Set<Role> roles;
}
