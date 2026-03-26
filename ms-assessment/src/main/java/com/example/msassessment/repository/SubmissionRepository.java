package com.example.msassessment.repository;

import com.example.msassessment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudentId(Long studentId);
    List<Submission> findByAssessmentId(Long assessmentId);
    Optional<Submission> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);
    long countByAssessmentIdAndStudentId(Long assessmentId, Long studentId);
    Optional<Submission> findByIdAndAssessmentId(Long id, Long assessmentId);
}