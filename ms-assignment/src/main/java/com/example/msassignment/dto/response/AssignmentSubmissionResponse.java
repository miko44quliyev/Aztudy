package com.example.msassignment.dto.response;

import com.example.msassignment.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssignmentSubmissionResponse {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String textAnswer;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private SubmissionStatus status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}