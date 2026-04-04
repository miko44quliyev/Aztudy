package com.example.mscontent.dto;

import com.example.mscontent.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {
    private Long id;
    private String title;
    private String description;
    private ContentType contentType;
    private String fileUrl;       // presigned URL
    private Long courseId;
    private Long teacherId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}