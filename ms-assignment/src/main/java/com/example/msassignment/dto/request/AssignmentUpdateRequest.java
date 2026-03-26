package com.example.msassignment.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentUpdateRequest {
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer maxScore;
}