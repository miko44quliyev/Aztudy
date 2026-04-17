package com.example.mscourse.controller;

import com.example.mscourse.dto.CourseRequest;
import com.example.mscourse.dto.CourseResponse;
import com.example.mscourse.dto.CourseUpdateRequest;
import com.example.mscourse.security.CustomUserPrincipal;
import com.example.mscourse.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        courseService.deleteCourse(courseId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(courseService.getAllCourses(principal.id()));
    }
}