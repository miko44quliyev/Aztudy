package com.example.mscourse.mapper;

import com.example.mscourse.dto.CourseRequest;
import com.example.mscourse.dto.CourseResponse;
import com.example.mscourse.dto.CourseUpdateRequest;
import com.example.mscourse.entity.Course;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    Course fromDto(CourseRequest request);

    CourseResponse toDto(Course course);

    void updateFromDto(CourseUpdateRequest request, @MappingTarget Course course);

    void updateFromDtoWithNull(CourseUpdateRequest request, @MappingTarget Course course);
}