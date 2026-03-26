package com.example.msassignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignmentGradeRequest {

    @NotNull(message = "Score is required")
    private Integer score;

    private String feedback;
}