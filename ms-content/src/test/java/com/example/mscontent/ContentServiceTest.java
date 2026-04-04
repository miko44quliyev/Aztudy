package com.example.mscontent;

import com.example.mscontent.client.CourseClient;
import com.example.mscontent.client.CourseResponse;
import com.example.mscontent.dto.*;
import com.example.mscontent.entity.Content;
import com.example.mscontent.exception.ResourceNotFoundException;
import com.example.mscontent.exception.UnauthorizedException;
import com.example.mscontent.mapper.ContentMapper;
import com.example.mscontent.repository.ContentRepository;
import com.example.mscontent.service.ContentService;
import com.example.mscontent.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private CourseClient courseClient;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private ContentService contentService;

    private Content content;
    private ContentResponse response;

    @BeforeEach
    void setUp() {
        content = new Content();
        content.setId(1L);
        content.setTeacherId(10L);
        content.setFileName("file.pdf");

        response = new ContentResponse();
    }

    // ─── UPLOAD ───
    @Test
    void uploadContent_success() {
        ContentUploadRequest request = new ContentUploadRequest();
        request.setCourseId(1L);
        request.setFile(file);

        CourseResponse course = new CourseResponse();
        course.setTeacherId(10L);

        when(courseClient.getCourseById(1L)).thenReturn(course);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(minioService.uploadFile(file, "course-1")).thenReturn("file.pdf");
        when(contentMapper.fromDto(request)).thenReturn(content);
        when(contentRepository.save(any())).thenReturn(content);
        when(contentMapper.toDto(content)).thenReturn(response);

        ContentResponse result = contentService.uploadContent(request, 10L);

        assertNotNull(result);
        verify(contentRepository).save(any());
    }

    @Test
    void uploadContent_unauthorized() {
        ContentUploadRequest request = new ContentUploadRequest();
        request.setCourseId(1L);

        CourseResponse course = new CourseResponse();
        course.setTeacherId(99L);

        when(courseClient.getCourseById(1L)).thenReturn(course);

        assertThrows(UnauthorizedException.class,
                () -> contentService.uploadContent(request, 10L));
    }

    // ─── GET BY ID ───
    @Test
    void getContentById_success() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));
        when(contentMapper.toDto(content)).thenReturn(response);

        ContentResponse result = contentService.getContentById(1L);

        assertNotNull(result);
    }

    @Test
    void getContentById_notFound() {
        when(contentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> contentService.getContentById(1L));
    }

    // ─── GET BY COURSE ───
    @Test
    void getContentByCourseId_success() {
        when(contentRepository.findByCourseId(1L))
                .thenReturn(List.of(content));
        when(contentMapper.toDto(content)).thenReturn(response);

        List<ContentResponse> result = contentService.getContentByCourseId(1L);

        assertEquals(1, result.size());
    }

    // ─── GET ALL ───
    @Test
    void getAllContents_success() {
        when(contentRepository.findAll()).thenReturn(List.of(content));
        when(contentMapper.toDto(content)).thenReturn(response);

        List<ContentResponse> result = contentService.getAllContents();

        assertEquals(1, result.size());
    }

    // ─── UPDATE ───
    @Test
    void updateContent_success() {
        ContentUpdateRequest request = new ContentUpdateRequest();

        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));
        when(contentRepository.save(content)).thenReturn(content);
        when(contentMapper.toDto(content)).thenReturn(response);

        ContentResponse result = contentService.updateContent(1L, request, 10L);

        assertNotNull(result);
        verify(contentMapper).updateFromDto(request, content);
    }

    @Test
    void updateContent_unauthorized() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));

        assertThrows(UnauthorizedException.class,
                () -> contentService.updateContent(1L, new ContentUpdateRequest(), 99L));
    }

    // ─── DELETE ───
    @Test
    void deleteContent_success() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));

        contentService.deleteContent(1L, 10L);

        verify(minioService).deleteFile("file.pdf");
        verify(contentRepository).delete(content);
    }

    @Test
    void deleteContent_unauthorized() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));

        assertThrows(UnauthorizedException.class,
                () -> contentService.deleteContent(1L, 99L));
    }

    // ─── COURSE NOT FOUND ───
    @Test
    void uploadContent_courseNotFound() {
        ContentUploadRequest request = new ContentUploadRequest();
        request.setCourseId(1L);

        when(courseClient.getCourseById(1L))
                .thenThrow(new RuntimeException());

        assertThrows(ResourceNotFoundException.class,
                () -> contentService.uploadContent(request, 10L));
    }
}