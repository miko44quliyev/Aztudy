package com.example.mscontent.service;

import com.example.mscontent.client.CourseClient;
import com.example.mscontent.client.CourseResponse;
import com.example.mscontent.dto.ContentResponse;
import com.example.mscontent.dto.ContentUpdateRequest;
import com.example.mscontent.dto.ContentUploadRequest;
import com.example.mscontent.entity.Content;
import com.example.mscontent.enums.ContentType;
import com.example.mscontent.exception.ResourceNotFoundException;
import com.example.mscontent.exception.UnauthorizedException;
import com.example.mscontent.mapper.ContentMapper;
import com.example.mscontent.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final MinioService minioService;
    private final ContentMapper contentMapper;
    private final CourseClient courseClient;

    // ─── internal entity fetch ───
    private Content fetchContentById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }

    // ─── UPLOAD ───
    @CacheEvict(value = {"contents", "contents_by_course"}, allEntries = true)
    @Transactional
    public ContentResponse uploadContent(ContentUploadRequest request, Long teacherId) {
        validateCourseOwnership(request.getCourseId(), teacherId);
        MultipartFile file = request.getFile();
        String folder = "course-" + request.getCourseId();
        String fileName = minioService.uploadFile(file, folder);

        Content content = contentMapper.fromDto(request);
        content.setFileName(fileName);
        content.setTeacherId(teacherId);
        content.setFileSize(file.getSize());
        content.setMimeType(file.getContentType());
        content.setContentType(detectContentType(file.getContentType()));

        return contentMapper.toDto(contentRepository.save(content));
    }

    // ─── GET BY ID ───
    @Cacheable(value = "contents", key = "#id")
    @Transactional(readOnly = true)
    public ContentResponse getContentById(Long id) {
        return contentMapper.toDto(fetchContentById(id));
    }

    // ─── GET BY COURSE ───
    @Cacheable(value = "contents_by_course", key = "#courseId")
    @Transactional(readOnly = true)
    public List<ContentResponse> getContentByCourseId(Long courseId) {
        return contentRepository.findByCourseId(courseId)
                .stream()
                .map(contentMapper::toDto)
                .toList();
    }

    // ─── GET ALL ───
    @Transactional(readOnly = true)
    public List<ContentResponse> getAllContents() {
        return contentRepository.findAll()
                .stream()
                .map(contentMapper::toDto)
                .toList();
    }

    // ─── UPDATE ───
    @CacheEvict(value = {"contents", "contents_by_course"}, allEntries = true)
    @Transactional
    public ContentResponse updateContent(Long id, ContentUpdateRequest request, Long teacherId) {
        Content content = fetchContentById(id);
        validateOwnership(content, teacherId);
        contentMapper.updateFromDto(request, content);
        return contentMapper.toDto(contentRepository.save(content));
    }

    // ─── DELETE ───
    @CacheEvict(value = {"contents", "contents_by_course"}, allEntries = true)
    @Transactional
    public void deleteContent(Long id, Long teacherId) {
        Content content = fetchContentById(id);
        validateOwnership(content, teacherId);
        minioService.deleteFile(content.getFileName());
        contentRepository.delete(content);
    }

    // ─── HELPERS ───
    private void validateOwnership(Content content, Long teacherId) {
        if (!content.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to modify this content");
        }
    }

    private void validateCourseOwnership(Long courseId, Long teacherId) {
        CourseResponse course;
        try {
            course = courseClient.getCourseById(courseId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to perform this action on this course");
        }
    }

    private ContentType detectContentType(String mimeType) {
        if (mimeType == null) return ContentType.DOCUMENT;
        if (mimeType.startsWith("video/")) return ContentType.VIDEO;
        if (mimeType.startsWith("image/")) return ContentType.IMAGE;
        if (mimeType.equals("application/pdf")) return ContentType.PDF;
        return ContentType.DOCUMENT;
    }
}