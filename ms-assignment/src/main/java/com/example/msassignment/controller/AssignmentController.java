package com.example.msassignment.controller;

import com.example.msassignment.dto.request.AssignmentGradeRequest;
import com.example.msassignment.dto.request.AssignmentRequest;
import com.example.msassignment.dto.request.AssignmentSubmissionRequest;
import com.example.msassignment.dto.request.AssignmentUpdateRequest;
import com.example.msassignment.dto.response.AssignmentResponse;
import com.example.msassignment.dto.response.AssignmentSubmissionResponse;
import com.example.msassignment.security.CustomUserPrincipal;
import com.example.msassignment.service.AssignmentService;
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
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Slf4j
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("📝 Creating assignment: {} by teacher: {}", request.getTitle(), teacher.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("✏️ Updating assignment: {} by teacher: {}", assignmentId, teacher.id());
        return ResponseEntity.ok(assignmentService.updateAssignment(assignmentId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("🗑️ Deleting assignment: {} by teacher: {}", assignmentId, teacher.id());
        assignmentService.deleteAssignment(assignmentId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(
            @PathVariable Long assignmentId) {
        log.info("📖 Getting assignment: {}", assignmentId);
        return ResponseEntity.ok(assignmentService.getAssignmentById(assignmentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByCourse(
            @PathVariable Long courseId) {
        log.info("📚 Getting assignments for course: {}", courseId);
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourse(courseId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
            @Valid @ModelAttribute AssignmentSubmissionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal student) {
        log.info("📤 Submitting assignment: {} by student: {}", request.getAssignmentId(), student.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.submitAssignment(request, student.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{assignmentId}/submissions/{submissionId}/grade")
    public ResponseEntity<AssignmentSubmissionResponse> gradeSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody AssignmentGradeRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("📝 Grading submission: {} by teacher: {}", submissionId, teacher.id());
        return ResponseEntity.ok(assignmentService.gradeSubmission(
                assignmentId, submissionId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getSubmissions(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("📊 Getting submissions for assignment: {}", assignmentId);
        return ResponseEntity.ok(assignmentService.getSubmissionsByAssignment(assignmentId, teacher.id()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-submissions")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal CustomUserPrincipal student) {
        log.info("📋 Getting submissions for student: {}", student.id());
        return ResponseEntity.ok(assignmentService.getMySubmissions(student.id()));
    }
}