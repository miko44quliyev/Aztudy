package com.example.mscontent.mapper;

import com.example.mscontent.dto.ContentResponse;
import com.example.mscontent.dto.ContentUpdateRequest;
import com.example.mscontent.dto.ContentUploadRequest;
import com.example.mscontent.entity.Content;
import com.example.mscontent.service.MinioService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ContentMapper {

    @Autowired
    protected MinioService minioService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Content fromDto(ContentUploadRequest request);

    @Mapping(target = "fileUrl", ignore = true)
    public abstract ContentResponse toDto(Content content);


    @AfterMapping
    protected void setFileUrl(Content content, @MappingTarget ContentResponse response) {
        response.setFileUrl(minioService.getPresignedUrl(content.getFileName()));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFromDto(ContentUpdateRequest request, @MappingTarget Content content);
}