package com.example.mscourse.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CourseResponse {
    private Long id;
    private Long teacherId;
    private String title;
    private String description;
    private String courseCode;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
