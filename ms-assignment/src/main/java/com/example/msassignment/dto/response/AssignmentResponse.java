package com.example.msassignment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Long teacherId;
    private LocalDateTime deadline;
    private Integer maxScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}