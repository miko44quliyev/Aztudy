package com.example.mscontent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFileInfo {
    private String fileName;
    private Long fileSize;
    private String contentType;
    private ZonedDateTime lastModified;
    private String etag;
}