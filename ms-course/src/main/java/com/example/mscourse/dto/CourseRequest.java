package com.example.mscourse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {
    private @NotBlank(message="title is required")
    @Size(min = 2, max = 50,message = "Title must be between 2 and 50 characters")
    String title;
    String description;
}
