package com.example.msassignment.mapper;

import com.example.msassignment.dto.request.AssignmentRequest;
import com.example.msassignment.dto.request.AssignmentUpdateRequest;
import com.example.msassignment.dto.response.AssignmentResponse;
import com.example.msassignment.dto.response.AssignmentSubmissionResponse;
import com.example.msassignment.entity.*;
import com.example.msassignment.service.MinioService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class AssignmentMapper {

    @Autowired
    protected MinioService minioService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Assignment fromDto(AssignmentRequest request);

    public abstract AssignmentResponse toDto(Assignment assignment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFromDto(AssignmentUpdateRequest request, @MappingTarget Assignment assignment);

    @Mapping(target = "fileUrl", ignore = true)
    public abstract AssignmentSubmissionResponse toDto(AssignmentSubmission submission);

    @AfterMapping
    protected void setFileUrl(AssignmentSubmission submission,
                               @MappingTarget AssignmentSubmissionResponse response) {
        if (submission.getFileName() != null) {
            response.setFileUrl(minioService.getPresignedUrl(submission.getFileName()));
        }
    }
}