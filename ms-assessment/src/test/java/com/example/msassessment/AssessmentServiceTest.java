package com.example.msassessment;

import com.example.msassessment.client.CourseClient;
import com.example.msassessment.client.CourseResponse;
import com.example.msassessment.dto.request.*;
import com.example.msassessment.dto.response.AssessmentResponse;
import com.example.msassessment.dto.response.SubmissionResponse;
import com.example.msassessment.entity.*;
import com.example.msassessment.enums.QuestionType;
import com.example.msassessment.exception.ResourceNotFoundException;
import com.example.msassessment.exception.UnauthorizedException;
import com.example.msassessment.mapper.AssessmentMapper;
import com.example.msassessment.repository.AssessmentRepository;
import com.example.msassessment.repository.SubmissionRepository;
import com.example.msassessment.service.AssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssessmentMapper assessmentMapper;
    @Mock private CourseClient courseClient;

    @InjectMocks private AssessmentService assessmentService;

    private Assessment assessment;
    private final Long teacherId = 1L;
    private final Long studentId = 2L;
    private final Long courseId = 10L;

    @BeforeEach
    void setUp() {
        // Mock Assessment obyekti yaradırıq
        assessment = new Assessment();
        assessment.setId(1L);
        assessment.setTeacherId(teacherId);
        assessment.setCourseId(courseId);
        assessment.setPassingScore(50);

        // Sual və Variant (Option) yaradırıq
        Question question = new Question();
        question.setId(10L);
        question.setPoints(20);
        question.setType(QuestionType.MCQ);

        Option correctOption = new Option();
        correctOption.setId(100L);
        correctOption.setIsCorrect(true);
        question.setOptions(Set.of(correctOption));

        assessment.setQuestions(Set.of(question));
    }

    @Test
    @DisplayName("Assessment yaradılması - Müəllim kursun sahibidirsə")
    void createAssessment_Success() {
        AssessmentRequest request = new AssessmentRequest();
        request.setCourseId(courseId);

        CourseResponse courseResponse = new CourseResponse();
        courseResponse.setTeacherId(teacherId);

        when(courseClient.getCourseById(courseId)).thenReturn(courseResponse);
        when(assessmentMapper.fromAssessmentDto(any())).thenReturn(assessment);
        when(assessmentRepository.save(any())).thenReturn(assessment);
        when(assessmentMapper.toAssessmentDto(any())).thenReturn(new AssessmentResponse());

        assertDoesNotThrow(() -> assessmentService.createAssessment(request, teacherId));
        verify(assessmentRepository).save(any());
    }

    @Test
    @DisplayName("Assessment gətirilməsi - ID üzrə")
    void getAssessmentById_Success() {
        when(assessmentRepository.findByIdWithQuestionsAndOptions(1L)).thenReturn(Optional.of(assessment));
        when(assessmentMapper.toAssessmentDto(any())).thenReturn(new AssessmentResponse());

        AssessmentResponse response = assessmentService.getAssessmentById(1L);
        assertNotNull(response);
        verify(assessmentRepository).findByIdWithQuestionsAndOptions(1L);
    }

    @Test
    @DisplayName("Submit Assessment - Score Hesablanması")
    void submitAssessment_CorrectScoreCalculation() {
        // 1. SETUP
        Long assessmentId = 1L;
        assessment.setId(assessmentId); // Mock obyekti mütləq ID-yə malik olmalıdır

        SubmissionRequest request = new SubmissionRequest();
        request.setAssessmentId(assessmentId);

        AnswerRequest aReq = new AnswerRequest();
        aReq.setQuestionId(10L);
        aReq.setSelectedOptionId(100L);
        request.setAnswers(List.of(aReq));

        // 2. STUBBING (Bura Diqqət!)
        // Repository-də @Param("id") olduğu üçün eq() və ya anyLong() istifadəsi daha etibarlıdır
        lenient().when(assessmentRepository.findByIdWithQuestionsAndOptions(anyLong()))
                .thenReturn(Optional.of(assessment));

        lenient().when(submissionRepository.countByAssessmentIdAndStudentId(anyLong(), anyLong()))
                .thenReturn(0L);

        lenient().when(submissionRepository.save(any(Submission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(assessmentMapper.toSubmissionDto(any()))
                .thenReturn(new SubmissionResponse());

        // 3. EXECUTE
        SubmissionResponse response = assessmentService.submitAssessment(request, studentId);

        // 4. VERIFY
        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());

        Submission savedSubmission = captor.getValue();

        // Əgər bura qədər gəlibsə, score-u yoxlayaq
        assertEquals(100, savedSubmission.getScore(), "Sual düzdürsə score 100 olmalıdır!");
    }

    @Test
    @DisplayName("Submit Assessment - Səhv cavab verildikdə score 0 olmalıdır")
    void submitAssessment_WrongAnswer_ScoreZero() {
        SubmissionRequest request = new SubmissionRequest();
        request.setAssessmentId(1L);

        AnswerRequest answerRequest = new AnswerRequest();
        answerRequest.setQuestionId(10L);
        answerRequest.setSelectedOptionId(999L); // Yanlış ID
        request.setAnswers(List.of(answerRequest));

        when(assessmentRepository.findByIdWithQuestionsAndOptions(1L)).thenReturn(Optional.of(assessment));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> i.getArguments()[0]);

        assessmentService.submitAssessment(request, studentId);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());

        assertEquals(0, captor.getValue().getScore());
        assertFalse(captor.getValue().getPassed());
    }

    @Test
    @DisplayName("Grade Submission - Müəllim əllə qiyməti AnswerGradeRequest ilə yeniləyir")
    void gradeSubmission_Success() {
        // GIVEN
        Submission submission = new Submission();
        submission.setId(5L);

        Answer answer = new Answer();
        answer.setQuestionId(10L);
        submission.setAnswers(new ArrayList<>(List.of(answer)));

        // Sənin strukturun: SubmissionGradeRequest -> List<AnswerGradeRequest>
        SubmissionGradeRequest gradeRequest = new SubmissionGradeRequest();

        AnswerGradeRequest agr = new AnswerGradeRequest();
        agr.setQuestionId(10L);
        agr.setPointsEarned(10); // 20 baldan 10 bal veririk (50%)
        agr.setIsCorrect(true);

        gradeRequest.setGrades(List.of(agr));

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(submissionRepository.findByIdAndAssessmentId(5L, 1L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenReturn(submission);
        when(assessmentMapper.toSubmissionDto(any())).thenReturn(new SubmissionResponse());

        // WHEN
        assessmentService.gradeSubmission(1L, 5L, gradeRequest, teacherId);

        // THEN
        verify(submissionRepository).save(submission);
        assertEquals(50, submission.getScore(), "20 baldan 10 bal alıbsa score 50% olmalıdır");
    }

    @Test
    @DisplayName("Delete Assessment - Sahibi olmayan silmək istədikdə xəta")
    void deleteAssessment_Unauthorized() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

        assertThrows(UnauthorizedException.class, () ->
                assessmentService.deleteAssessment(1L, 99L) // Başqa müəllim ID-si
        );
    }
}