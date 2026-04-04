package com.example.msassignment.controller;

import com.example.msassignment.dto.request.*;
import com.example.msassignment.dto.response.AssignmentResponse;
import com.example.msassignment.dto.response.AssignmentSubmissionResponse;
import com.example.msassignment.exception.ResourceNotFoundException;
import com.example.msassignment.exception.UnauthorizedException;
import com.example.msassignment.security.CustomUserPrincipal;
import com.example.msassignment.service.AssignmentService;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PatchMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(assignmentService.updateAssignment(assignmentId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        assignmentService.deleteAssignment(assignmentId, teacher.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(assignmentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourse(courseId));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
            @Valid @ModelAttribute AssignmentSubmissionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal student) {
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
        return ResponseEntity.ok(assignmentService.gradeSubmission(
                assignmentId, submissionId, request, teacher.id()));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getSubmissions(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserPrincipal teacher) {
        return ResponseEntity.ok(assignmentService.getSubmissionsByAssignment(assignmentId, teacher.id()));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-submissions")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal CustomUserPrincipal student) {
        return ResponseEntity.ok(assignmentService.getMySubmissions(student.id()));
    }
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or hasRole('STUDENT')")
    @GetMapping("/submissions/{submissionId}/view")
    public ResponseEntity<InputStreamResource> viewSubmissionFile(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        log.info("👁️ Viewing submission file: submissionId={}, userId={}", submissionId, user.id());

        try {
            InputStream inputStream = assignmentService.downloadSubmissionFile(submissionId, user.id());
            String fileName = assignmentService.getSubmissionFileName(submissionId);
            String contentType = assignmentService.getSubmissionContentType(submissionId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(inputStream));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // ─── STUDENT UPDATE THEIR OWN SUBMISSION ───
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping(value = "/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentSubmissionResponse> updateSubmission(
            @PathVariable Long submissionId,
            @Valid @ModelAttribute AssignmentSubmissionUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal student) {


        return ResponseEntity.ok(assignmentService.updateSubmission(submissionId, request, student.id()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/submissions/{submissionId}/edit")
    public ResponseEntity<AssignmentSubmissionResponse> getSubmissionForEdit(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal CustomUserPrincipal student) {


        return ResponseEntity.ok(assignmentService.getSubmissionForEdit(submissionId, student.id()));
    }
}