package com.example.msassessment.dto.request;

import lombok.Data;

@Data
public class AssessmentUpdateRequest {
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer passingScore;
    private Integer maxAttempts;
    private Boolean isActive;
}