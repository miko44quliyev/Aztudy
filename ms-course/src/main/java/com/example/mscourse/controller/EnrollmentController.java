// EnrollmentController.java
package com.example.mscourse.controller;

import com.example.mscourse.entity.Enrollment;
import com.example.mscourse.security.CustomUserPrincipal;
import com.example.mscourse.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Tələbə courseCode ilə qoşulur
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/join")
    public ResponseEntity<Enrollment> joinByCourseCode(
            @RequestParam String courseCode,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        Enrollment enrollment = enrollmentService.enrollByCode(courseCode, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping("/courses/{courseId}/add-student")
    public ResponseEntity<Enrollment> addStudentByEmail(
            @PathVariable Long courseId,
            @RequestParam String email,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        Enrollment enrollment = enrollmentService.enrollStudentByEmail(courseId, email, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(principal.id());
        return ResponseEntity.ok(enrollments);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> withdrawEnrollment(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        enrollmentService.withdrawEnrollment(courseId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<List<Enrollment>> getCourseStudents(@PathVariable Long courseId) {
        List<Enrollment> enrollments = enrollmentService.getCourseEnrollments(courseId);
        return ResponseEntity.ok(enrollments);
    }
}