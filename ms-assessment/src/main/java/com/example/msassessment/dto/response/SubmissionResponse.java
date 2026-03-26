package com.example.msassessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long assessmentId;
    private Long studentId;
    private Integer score;
    private Boolean passed;
    private List<AnswerResponse> answers;
    private LocalDateTime submittedAt;
}