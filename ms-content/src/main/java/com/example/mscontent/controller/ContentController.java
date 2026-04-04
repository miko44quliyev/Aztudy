package com.example.mscontent.controller;

import com.example.mscontent.dto.ContentFileInfo;
import com.example.mscontent.dto.ContentResponse;
import com.example.mscontent.dto.ContentUpdateRequest;
import com.example.mscontent.dto.ContentUploadRequest;
import com.example.mscontent.security.CustomUserPrincipal;
import com.example.mscontent.service.ContentService;
import com.example.mscontent.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

    private final ContentService contentService;
    private final MinioService minioService;

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> uploadContent(
            @Valid @ModelAttribute ContentUploadRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("📤 Uploading content: {} by teacher: {}", request.getTitle(), teacher.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.uploadContent(request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> updateContent(
            @PathVariable Long contentId,
            @Valid @ModelAttribute ContentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(contentService.updateContent(contentId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        contentService.deleteContent(contentId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentResponse> getContentById(@PathVariable Long contentId) {
        return ResponseEntity.ok(contentService.getContentById(contentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ContentResponse>> getContentByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(contentService.getContentByCourseId(courseId));
    }

    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ContentResponse>> getAllContents(
            @AuthenticationPrincipal CustomUserPrincipal user) {
        return ResponseEntity.ok(contentService.getAllContents());
    }


    /**
     * Faylı birbaşa brauzerdə açmaq üçün (inline)
     * Şəkil, PDF, video, audio faylları brauzerdə göstərir
     */
    @GetMapping("/view/{contentId}")
    public ResponseEntity<InputStreamResource> viewFile(@PathVariable Long contentId) {

        try {
            // Content məlumatlarını al
            ContentResponse content = contentService.getContentById(contentId);

            // MinIO-dan faylı yüklə
            InputStream inputStream = minioService.downloadFile(content.getFileName());

            // Content type-i təyin et
            MediaType mediaType = MediaType.parseMediaType(content.getMimeType());

            // INLINE - brauzerdə açılması üçün
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
                    .contentType(mediaType)
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Faylı yükləmək üçün (attachment - brauzer yükləyəcək)
     */
    @GetMapping("/download/{contentId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long contentId) {

        try {
            ContentResponse content = contentService.getContentById(contentId);
            InputStream inputStream = minioService.downloadFile(content.getFileName());
            MediaType mediaType = MediaType.parseMediaType(content.getMimeType());

            // ATTACHMENT - yüklənməsi üçün
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.getFileName() + "\"")
                    .contentType(mediaType)
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Presigned URL qaytarır (birbaşa MinIO-dan)
     * Frontend bu URL-i istifadə edərək faylı aça bilər
     */
    @GetMapping("/url/{contentId}")
    public ResponseEntity<String> getFileUrl(@PathVariable Long contentId) {
        log.info("🔗 Getting file URL for content: {}", contentId);

        try {
            ContentResponse content = contentService.getContentById(contentId);
            String url = minioService.getPresignedUrl(content.getFileName(), 3600);
            return ResponseEntity.ok(url);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Fayl haqqında metadata məlumatları
     */
    @GetMapping("/info/{contentId}")
    public ResponseEntity<ContentFileInfo> getFileInfo(@PathVariable Long contentId) {

        try {
            ContentResponse content = contentService.getContentById(contentId);
            ContentFileInfo fileInfo = minioService.getFileInfo(content.getFileName());
            return ResponseEntity.ok(fileInfo);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}