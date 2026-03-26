package com.example.msassessment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerGradeRequest {

    @NotNull
    private Long questionId;

    @NotNull
    private Integer pointsEarned;

    private Boolean isCorrect;
}