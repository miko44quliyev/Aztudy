package com.example.msassignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AssignmentSubmissionRequest {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    private String textAnswer;
    private MultipartFile file;
}