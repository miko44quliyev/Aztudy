package com.example.msassessment.controller;

import com.example.msassessment.dto.request.AssessmentRequest;
import com.example.msassessment.dto.request.AssessmentUpdateRequest;
import com.example.msassessment.dto.request.SubmissionGradeRequest;
import com.example.msassessment.dto.request.SubmissionRequest;
import com.example.msassessment.dto.response.AssessmentResponse;
import com.example.msassessment.dto.response.SubmissionResponse;
import com.example.msassessment.enums.AssessmentType;
import com.example.msassessment.security.CustomUserPrincipal;
import com.example.msassessment.service.AssessmentService;
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
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AssessmentResponse> createAssessment(
            @Valid @RequestBody AssessmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.createAssessment(request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{assessmentId}")
    public ResponseEntity<AssessmentResponse> updateAssessment(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        log.info("✏️ Updating assessment: {} by teacher: {}", assessmentId, teacher.id());
        return ResponseEntity.ok(assessmentService.updateAssessment(assessmentId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{assessmentId}")
    public ResponseEntity<Void> deleteAssessment(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        assessmentService.deleteAssessment(assessmentId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assessmentId}")
    public ResponseEntity<AssessmentResponse> getAssessmentById(
            @PathVariable Long assessmentId) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(assessmentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AssessmentResponse>> getAssessmentsByCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) AssessmentType type) {
        List<AssessmentResponse> assessments = type != null
                ? assessmentService.getAssessmentsByCourseAndType(courseId, type)
                : assessmentService.getAssessmentsByCourse(courseId);
        return ResponseEntity.ok(assessments);
    }

    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping("/submit")
    public ResponseEntity<SubmissionResponse> submitAssessment(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal student) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.submitAssessment(request, student.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/{assessmentId}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(
            @PathVariable Long assessmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(assessmentService.getSubmissionsByAssessment(assessmentId, teacher.id()));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-submissions")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal CustomUserPrincipal student) {
        return ResponseEntity.ok(assessmentService.getMySubmissions(student.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{assessmentId}/submissions/{submissionId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long assessmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody SubmissionGradeRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(assessmentService.gradeSubmission(
                assessmentId, submissionId, request, teacher.id()));
    }
}