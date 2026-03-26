package com.example.msauth.service;

import com.example.msauth.dto.UserRegisterRequest;
import com.example.msauth.entity.Role;
import com.example.msauth.entity.User;
import com.example.msauth.exception.ResourceNotFoundException;
import com.example.msauth.exception.UserAlreadyExistsException;
import com.example.msauth.repository.RoleRepository;
import com.example.msauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    @Transactional
    public User register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        Set<Role> roles = new HashSet<>();

        String roleName = "TEACHER".equalsIgnoreCase(request.getRole())
                ? "ROLE_TEACHER"
                : "ROLE_USER";

        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}