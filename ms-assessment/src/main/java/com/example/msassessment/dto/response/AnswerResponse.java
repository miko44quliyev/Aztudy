package com.example.msassessment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long questionId;
    private Long selectedOptionId;
    private String textAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;
}