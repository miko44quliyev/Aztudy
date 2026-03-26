// AssessmentMapper.java
package com.example.msassessment.mapper;

import com.example.msassessment.dto.request.AssessmentRequest;
import com.example.msassessment.dto.request.AssessmentUpdateRequest;
import com.example.msassessment.dto.request.OptionRequest;
import com.example.msassessment.dto.request.QuestionRequest;
import com.example.msassessment.dto.response.*;
import com.example.msassessment.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Assessment fromAssessmentDto(AssessmentRequest request);

    AssessmentResponse toAssessmentDto(Assessment assessment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assessment", ignore = true)
    Question fromQuestionDto(QuestionRequest request);

    QuestionResponse toQuestionDto(Question question);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    Option fromOptionDto(OptionRequest request);

    SubmissionResponse toSubmissionDto(Submission submission);
    AnswerResponse toAnswerDto(Answer answer);
    OptionResponse toOptionDto(Option option);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromAssessmentDto(AssessmentUpdateRequest request, @MappingTarget Assessment assessment);
}