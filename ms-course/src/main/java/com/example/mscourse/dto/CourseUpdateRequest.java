package com.example.mscourse.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseUpdateRequest {
    @Size(min = 2, max = 50)
    private String title;

    private String description;
}
