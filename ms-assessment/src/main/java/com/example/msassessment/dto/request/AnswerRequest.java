// AnswerRequest.java
package com.example.msassessment.dto.request;

import lombok.Data;

@Data
public class AnswerRequest {
    private Long questionId;
    private Long selectedOptionId;
    private String textAnswer;
}