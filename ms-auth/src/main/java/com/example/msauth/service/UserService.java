package com.example.msauth.service;

import com.example.msauth.dto.UserRegisterRequest;
import com.example.msauth.dto.UserResponse;
import com.example.msauth.dto.UserUpdateRequest;
import com.example.msauth.entity.Role;
import com.example.msauth.entity.User;
import com.example.msauth.exception.ResourceNotFoundException;
import com.example.msauth.exception.UserAlreadyExistsException;
import com.example.msauth.mapper.UserMapper;
import com.example.msauth.repository.RoleRepository;
import com.example.msauth.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    // ─── internal entity fetch ───
    private User fetchUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── REGISTER ───
    @CacheEvict(value = {"users", "users_by_email"}, allEntries = true)
    public UserResponse registerUser(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use");
        }
        User user = userMapper.fromDto(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.getRoles().add(defaultRole);
        return userMapper.toDto(userRepository.save(user));
    }

    // ─── GET BY ID ───
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        return userMapper.toDto(fetchUserById(id));
    }

    // ─── UPDATE PARTIAL ───
    @CacheEvict(value = {"users", "users_by_email"}, allEntries = true)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = fetchUserById(id);
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        userMapper.updateFromDto(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    // ─── UPDATE FULLY ───
    @CacheEvict(value = {"users", "users_by_email"}, allEntries = true)
    public UserResponse updateUserFully(Long id, UserUpdateRequest request) {
        User user = fetchUserById(id);
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use");
        }
        userMapper.updateFromDtoWithNull(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    // ─── DELETE ───
    @CacheEvict(value = {"users", "users_by_email"}, allEntries = true)
    public void deleteUserById(Long id) {
        userRepository.delete(fetchUserById(id));
    }

    // ─── FIND BY EMAIL ───
    @Cacheable(value = "users_by_email", key = "#email")
    public UserResponse findByEmail(String email) {
        return userMapper.toDto(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email)));
    }

    // ─── internal — cache-lənmir, entity qaytarır ───
    public User findUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}