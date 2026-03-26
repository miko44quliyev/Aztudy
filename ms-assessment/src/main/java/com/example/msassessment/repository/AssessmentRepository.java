package com.example.msassessment.repository;

import com.example.msassessment.entity.Assessment;
import com.example.msassessment.enums.AssessmentType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    @EntityGraph(attributePaths = {"questions", "questions.options"})
    List<Assessment> findByCourseId(Long courseId);
    @EntityGraph(attributePaths = {"questions", "questions.options"})
    List<Assessment> findByCourseIdAndType(Long courseId, AssessmentType type);
    @EntityGraph(attributePaths = {"questions", "questions.options"})
    @Query("SELECT a FROM Assessment a WHERE a.id = :id")
    Optional<Assessment> findByIdWithQuestionsAndOptions(@Param("id") Long id);
}