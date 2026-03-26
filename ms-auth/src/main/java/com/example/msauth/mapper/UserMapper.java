package com.example.msauth.mapper;

import com.example.msauth.dto.UserRegisterRequest;
import com.example.msauth.dto.UserResponse;
import com.example.msauth.dto.UserUpdateRequest;
import com.example.msauth.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toDto(User user);
    User fromDto(UserRegisterRequest request);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UserUpdateRequest request, @MappingTarget User user);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateFromDtoWithNull(UserUpdateRequest request, @MappingTarget User user);
    
}
