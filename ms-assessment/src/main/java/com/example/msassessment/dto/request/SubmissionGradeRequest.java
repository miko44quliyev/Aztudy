package com.example.msassessment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmissionGradeRequest {

    @NotNull
    private List<AnswerGradeRequest> grades;
}