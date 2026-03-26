package com.example.mscontent.controller;

import com.example.mscontent.dto.ContentResponse;
import com.example.mscontent.dto.ContentUpdateRequest;
import com.example.mscontent.dto.ContentUploadRequest;
import com.example.mscontent.security.CustomUserPrincipal;
import com.example.mscontent.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

    private final ContentService contentService;

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
    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentResponse> updateContent(
            @PathVariable Long contentId,
            @Valid @RequestBody ContentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("✏️ Updating content: {} by teacher: {}", contentId, teacher.id());
        return ResponseEntity.ok(contentService.updateContent(contentId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("🗑️ Deleting content: {} by teacher: {}", contentId, teacher.id());
        contentService.deleteContent(contentId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentResponse> getContentById(@PathVariable Long contentId) {
        log.info("📖 Getting content: {}", contentId);
        return ResponseEntity.ok(contentService.getContentById(contentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ContentResponse>> getContentByCourse(@PathVariable Long courseId) {
        log.info("📚 Getting contents for course: {}", courseId);
        return ResponseEntity.ok(contentService.getContentByCourseId(courseId));
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ContentResponse>> getAllContents(
            @AuthenticationPrincipal CustomUserPrincipal user) {
        log.info("📚 Getting all contents for user: {}", user.id());
        return ResponseEntity.ok(contentService.getAllContents());
    }
}