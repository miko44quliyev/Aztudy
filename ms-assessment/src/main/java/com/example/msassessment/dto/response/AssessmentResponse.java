package com.example.msassessment.dto.response;

import com.example.msassessment.enums.AssessmentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {
    private Long id;
    private String title;
    private String description;
    private AssessmentType type;
    private Long courseId;
    private Long teacherId;
    private Integer durationMinutes;
    private Integer passingScore;
    private Integer maxAttempts;
    private Boolean isActive;
    private List<QuestionResponse> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}