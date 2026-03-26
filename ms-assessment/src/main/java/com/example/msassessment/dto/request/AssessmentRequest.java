package com.example.msassessment.dto.request;

import com.example.msassessment.enums.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssessmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Type is required")
    private AssessmentType type;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private Integer durationMinutes;
    private Integer passingScore;
    private Integer maxAttempts;

    private List<QuestionRequest> questions;
}