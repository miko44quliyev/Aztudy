package com.example.msassessment.service;

import com.example.msassessment.client.CourseClient;
import com.example.msassessment.client.CourseResponse;
import com.example.msassessment.dto.request.AssessmentRequest;
import com.example.msassessment.dto.request.AssessmentUpdateRequest;
import com.example.msassessment.dto.request.SubmissionGradeRequest;
import com.example.msassessment.dto.request.SubmissionRequest;
import com.example.msassessment.dto.response.AssessmentResponse;
import com.example.msassessment.dto.response.SubmissionResponse;
import com.example.msassessment.entity.*;
import com.example.msassessment.enums.AssessmentType;
import com.example.msassessment.enums.QuestionType;
import com.example.msassessment.exception.ResourceNotFoundException;
import com.example.msassessment.exception.UnauthorizedException;
import com.example.msassessment.mapper.AssessmentMapper;
import com.example.msassessment.repository.AssessmentRepository;
import com.example.msassessment.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssessmentMapper assessmentMapper;
    private final CourseClient courseClient;

    private Assessment fetchAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + id));
    }

    // ─── CREATE ───
    @CacheEvict(value = {"assessments", "assessments_by_course"}, allEntries = true)
    @Transactional
    public AssessmentResponse createAssessment(AssessmentRequest request, Long teacherId) {
        validateCourseOwnership(request.getCourseId(), teacherId);

        // fromDto -> fromAssessmentDto
        Assessment assessment = assessmentMapper.fromAssessmentDto(request);
        assessment.setTeacherId(teacherId);

        if (request.getQuestions() != null) {
            Set<Question> questions = request.getQuestions().stream().map(qReq -> {
                // fromDto -> fromQuestionDto
                Question question = assessmentMapper.fromQuestionDto(qReq);
                question.setAssessment(assessment);
                if (qReq.getOptions() != null) {
                    Set<Option> options = qReq.getOptions().stream().map(oReq -> {
                        // fromDto -> fromOptionDto
                        Option option = assessmentMapper.fromOptionDto(oReq);
                        option.setQuestion(question);
                        return option;
                    }).collect(Collectors.toSet());
                    question.setOptions(options);
                }
                return question;
            }).collect(Collectors.toSet());
            assessment.setQuestions(questions);
        }

        // toDto -> toAssessmentDto
        return assessmentMapper.toAssessmentDto(assessmentRepository.save(assessment));
    }

    // ─── GET BY ID ───
    @Cacheable(value = "assessments", key = "#id")
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentById(Long id) {
        return assessmentMapper.toAssessmentDto(
                assessmentRepository.findByIdWithQuestionsAndOptions(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + id))
        );
    }

    // ─── GET BY COURSE ───
    @Cacheable(value = "assessments_by_course", key = "#courseId")
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsByCourse(Long courseId) {
        return assessmentRepository.findByCourseId(courseId)
                .stream()
                .map(assessmentMapper::toAssessmentDto)
                .toList();
    }

    // ─── GET BY COURSE AND TYPE ───
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessmentsByCourseAndType(Long courseId, AssessmentType type) {
        return assessmentRepository.findByCourseIdAndType(courseId, type)
                .stream()
                .map(assessmentMapper::toAssessmentDto)
                .toList();
    }

    // ─── GET ALL ───
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAllAssessments() {
        return assessmentRepository.findAll()
                .stream()
                .map(assessmentMapper::toAssessmentDto)
                .toList();
    }

    // ─── UPDATE ───
    @CacheEvict(value = {"assessments", "assessments_by_course"}, allEntries = true)
    @Transactional
    public AssessmentResponse updateAssessment(Long id, AssessmentUpdateRequest request, Long teacherId) {
        Assessment assessment = fetchAssessmentById(id);
        validateOwnership(assessment, teacherId);
        // updateFromDto -> updateFromAssessmentDto
        assessmentMapper.updateFromAssessmentDto(request, assessment);
        return assessmentMapper.toAssessmentDto(assessmentRepository.save(assessment));
    }

    // ─── DELETE ───
    @CacheEvict(value = {"assessments", "assessments_by_course"}, allEntries = true)
    @Transactional
    public void deleteAssessment(Long id, Long teacherId) {
        Assessment assessment = fetchAssessmentById(id);
        validateOwnership(assessment, teacherId);
        assessmentRepository.delete(assessment);
    }

    // ─── SUBMIT ───
    @Transactional
    public SubmissionResponse submitAssessment(SubmissionRequest request, Long studentId) {
        Assessment assessment = assessmentRepository.findByIdWithQuestionsAndOptions(request.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        if (assessment.getMaxAttempts() != null) {
            long attempts = submissionRepository.countByAssessmentIdAndStudentId(
                    assessment.getId(), studentId);
            if (attempts >= assessment.getMaxAttempts()) {
                throw new UnauthorizedException("Maximum attempt limit reached");
            }
        }

        List<Answer> answers = request.getAnswers().stream().map(aReq -> {
            Question question = assessment.getQuestions().stream()
                    .filter(q -> q.getId().equals(aReq.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

            Answer answer = new Answer();
            answer.setQuestionId(aReq.getQuestionId());
            answer.setSelectedOptionId(aReq.getSelectedOptionId());
            answer.setTextAnswer(aReq.getTextAnswer());

            if (question.getType() == QuestionType.MCQ || question.getType() == QuestionType.TRUE_FALSE) {
                if (aReq.getSelectedOptionId() == null) {
                    answer.setIsCorrect(false);
                    answer.setPointsEarned(0);
                } else {
                    boolean correct = question.getOptions().stream()
                            .anyMatch(o -> o.getId().equals(aReq.getSelectedOptionId())
                                    && Boolean.TRUE.equals(o.getIsCorrect()));
                    answer.setIsCorrect(correct);
                    answer.setPointsEarned(correct ? (question.getPoints() != null ? question.getPoints() : 0) : 0);
                }
            } else {
                answer.setIsCorrect(null);
                answer.setPointsEarned(0);
            }
            return answer;
        }).toList();

        int totalScore = answers.stream()
                .mapToInt(a -> a.getPointsEarned() != null ? a.getPointsEarned() : 0)
                .sum();
        int maxScore = assessment.getQuestions().stream()
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int scorePercent = maxScore > 0 ? (totalScore * 100) / maxScore : 0;
        boolean passed = assessment.getPassingScore() == null || scorePercent >= assessment.getPassingScore();

        Submission submission = Submission.builder()
                .assessmentId(assessment.getId())
                .studentId(studentId)
                .score(scorePercent)
                .passed(passed)
                .answers(answers)
                .build();
        answers.forEach(a -> a.setSubmission(submission));

        // toDto -> toSubmissionDto
        return assessmentMapper.toSubmissionDto(submissionRepository.save(submission));
    }

    // ─── GET SUBMISSIONS BY ASSESSMENT ───
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssessment(Long assessmentId, Long teacherId) {
        Assessment assessment = fetchAssessmentById(assessmentId);
        validateOwnership(assessment, teacherId);
        return submissionRepository.findByAssessmentId(assessmentId)
                .stream()
                .map(assessmentMapper::toSubmissionDto) // toDto -> toSubmissionDto
                .toList();
    }

    // ─── GET MY SUBMISSIONS ───
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMySubmissions(Long studentId) {
        return submissionRepository.findByStudentId(studentId)
                .stream()
                .map(assessmentMapper::toSubmissionDto) // toDto -> toSubmissionDto
                .toList();
    }

    // ─── GRADE SUBMISSION ───
    @Transactional
    public SubmissionResponse gradeSubmission(Long assessmentId, Long submissionId,
                                              SubmissionGradeRequest request, Long teacherId) {
        Assessment assessment = fetchAssessmentById(assessmentId);
        validateOwnership(assessment, teacherId);

        Submission submission = submissionRepository.findByIdAndAssessmentId(submissionId, assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        request.getGrades().forEach(grade ->
                submission.getAnswers().stream()
                        .filter(a -> a.getQuestionId().equals(grade.getQuestionId()))
                        .findFirst()
                        .ifPresent(answer -> {
                            answer.setPointsEarned(grade.getPointsEarned());
                            answer.setIsCorrect(grade.getIsCorrect());
                        })
        );

        int totalScore = submission.getAnswers().stream()
                .mapToInt(a -> a.getPointsEarned() != null ? a.getPointsEarned() : 0)
                .sum();
        int maxScore = assessment.getQuestions().stream()
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int scorePercent = maxScore > 0 ? (totalScore * 100) / maxScore : 0;
        boolean passed = assessment.getPassingScore() == null || scorePercent >= assessment.getPassingScore();

        submission.setScore(scorePercent);
        submission.setPassed(passed);

        // toDto -> toSubmissionDto
        return assessmentMapper.toSubmissionDto(submissionRepository.save(submission));
    }

    private void validateOwnership(Assessment assessment, Long teacherId) {
        if (!assessment.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to modify this assessment");
        }
    }

    private void validateCourseOwnership(Long courseId, Long teacherId) {
        CourseResponse course;
        try {
            course = courseClient.getCourseById(courseId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to perform this action on this course");
        }
    }
}