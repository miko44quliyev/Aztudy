package com.example.mscontent.dto;

import com.example.mscontent.enums.ContentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContentResponse {
    private Long id;
    private String title;
    private String description;
    private ContentType contentType;
    private String fileUrl;       // presigned URL
    private Long courseId;
    private Long teacherId;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}