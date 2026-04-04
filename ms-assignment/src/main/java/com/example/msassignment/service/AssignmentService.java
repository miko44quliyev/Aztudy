package com.example.msassignment.service;

import com.example.msassignment.client.CourseClient;
import com.example.msassignment.client.CourseResponse;
import com.example.msassignment.dto.request.*;
import com.example.msassignment.dto.response.AssignmentResponse;
import com.example.msassignment.dto.response.AssignmentSubmissionResponse;
import com.example.msassignment.entity.*;
import com.example.msassignment.enums.SubmissionStatus;
import com.example.msassignment.exception.ResourceNotFoundException;
import com.example.msassignment.exception.UnauthorizedException;
import com.example.msassignment.mapper.AssignmentMapper;
import com.example.msassignment.repository.AssignmentRepository;
import com.example.msassignment.repository.AssignmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentMapper assignmentMapper;
    private final MinioService minioService;
    private final CourseClient courseClient;

    // ─── internal entity fetch ───
    private Assignment fetchAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
    }

    private AssignmentSubmission fetchSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + id));
    }

    // ─── CREATE ───
    @CacheEvict(value = {"assignments", "assignments_by_course"}, allEntries = true)
    @Transactional
    public AssignmentResponse createAssignment(AssignmentRequest request, Long teacherId) {
        validateCourseOwnership(request.getCourseId(), teacherId);
        Assignment assignment = assignmentMapper.fromDto(request);
        assignment.setTeacherId(teacherId);
        return assignmentMapper.toDto(assignmentRepository.save(assignment));
    }

    // ─── GET BY ID ───
    @Cacheable(value = "assignments", key = "#id")
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        return assignmentMapper.toDto(fetchAssignmentById(id));
    }

    // ─── GET BY COURSE ───
    @Cacheable(value = "assignments_by_course", key = "#courseId")
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByCourse(Long courseId) {
        return assignmentRepository.findByCourseId(courseId)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    // ─── GET ALL ───
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    // ─── UPDATE ───
    @CacheEvict(value = {"assignments", "assignments_by_course"}, allEntries = true)
    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentUpdateRequest request, Long teacherId) {
        Assignment assignment = fetchAssignmentById(id);
        validateOwnership(assignment, teacherId);
        assignmentMapper.updateFromDto(request, assignment);
        return assignmentMapper.toDto(assignmentRepository.save(assignment));
    }

    // ─── DELETE ───
    @CacheEvict(value = {"assignments", "assignments_by_course"}, allEntries = true)
    @Transactional
    public void deleteAssignment(Long id, Long teacherId) {
        Assignment assignment = fetchAssignmentById(id);
        validateOwnership(assignment, teacherId);
        assignmentRepository.delete(assignment);
    }
    // ─── UPDATE SUBMISSION (Student can update their own submission) ───
    @Transactional
    public AssignmentSubmissionResponse updateSubmission(Long submissionId,
                                                         AssignmentSubmissionUpdateRequest request,
                                                         Long studentId) {
        // Submission-u tap
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

        // Yalnız öz submission-unu dəyişə bilər
        if (!submission.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("You can only update your own submission");
        }

        // Assignment-ı tap
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        // Deadline keçibsə, yeniləməyə icazə vermə
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new UnauthorizedException("Cannot update submission after deadline");
        }

        // Mətn cavabını yenilə
        if (request.getTextAnswer() != null) {
            submission.setTextAnswer(request.getTextAnswer());
        }

        // Faylı yenilə (əgər yeni fayl gəlibsə)
        MultipartFile newFile = request.getFile();
        if (newFile != null && !newFile.isEmpty()) {
            // Köhnə faylı sil
            if (submission.getFileName() != null) {
                try {
                    minioService.deleteFile(submission.getFileName());
                } catch (Exception ignored) {
                }
            }

            // Yeni faylı yüklə
            String folder = "assignment-" + submission.getAssignmentId();
            String newFileName = minioService.uploadFile(newFile, folder);

            submission.setFileName(newFileName);
            submission.setFileSize(newFile.getSize());
            submission.setMimeType(newFile.getContentType());

        }

        // Status-u yenilə (yenidən SUBMITTED et)
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setUpdatedAt(LocalDateTime.now());

        // Qiyməti sıfırla (çünki yenidən yoxlanmalıdır)
        submission.setScore(null);
        submission.setFeedback(null);

        return assignmentMapper.toDto(submissionRepository.save(submission));
    }

    // ─── GET SUBMISSION BY ID (for edit) ───
    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getSubmissionForEdit(Long submissionId, Long studentId) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

        if (!submission.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("You can only view your own submission");
        }

        return assignmentMapper.toDto(submission);
    }
    // ─── SUBMIT ───
    @Transactional
    public AssignmentSubmissionResponse submitAssignment(AssignmentSubmissionRequest request, Long studentId) {
        Assignment assignment = fetchAssignmentById(request.getAssignmentId());

        if (submissionRepository.existsByAssignmentIdAndStudentId(assignment.getId(), studentId)) {
            throw new UnauthorizedException("You have already submitted this assignment");
        }

        SubmissionStatus status = SubmissionStatus.SUBMITTED;
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            status = SubmissionStatus.LATE;
        }

        String fileName = null;
        String mimeType = null;
        Long fileSize = null;

        MultipartFile file = request.getFile();
        if (file != null && !file.isEmpty()) {
            String folder = "assignment-" + assignment.getId();
            fileName = minioService.uploadFile(file, folder);
            mimeType = file.getContentType();
            fileSize = file.getSize();
        }

        AssignmentSubmission submission = AssignmentSubmission.builder()
                .assignmentId(assignment.getId())
                .studentId(studentId)
                .textAnswer(request.getTextAnswer())
                .fileName(fileName)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .status(status)
                .build();

        return assignmentMapper.toDto(submissionRepository.save(submission));
    }

    // ─── GRADE ───
    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(Long assignmentId, Long submissionId,
                                                        AssignmentGradeRequest request, Long teacherId) {
        Assignment assignment = fetchAssignmentById(assignmentId);
        validateOwnership(assignment, teacherId);

        AssignmentSubmission submission = fetchSubmissionById(submissionId);

        // Submission-in bu assignment-a aid olduğunu yoxla
        if (!submission.getAssignmentId().equals(assignmentId)) {
            throw new UnauthorizedException("Submission does not belong to this assignment");
        }

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);

        return assignmentMapper.toDto(submissionRepository.save(submission));
    }

    // ─── GET SUBMISSIONS BY ASSIGNMENT ───
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsByAssignment(Long assignmentId, Long teacherId) {
        Assignment assignment = fetchAssignmentById(assignmentId);
        validateOwnership(assignment, teacherId);
        return submissionRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    // ─── GET MY SUBMISSIONS ───
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getMySubmissions(Long studentId) {
        return submissionRepository.findByStudentId(studentId)
                .stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    // ==================== YENİ METODLAR ====================

    // ─── DOWNLOAD SUBMISSION FILE (View üçün) ───
    @Transactional(readOnly = true)
    public InputStream downloadSubmissionFile(Long submissionId, Long userId) {
        AssignmentSubmission submission = fetchSubmissionById(submissionId);
        Assignment assignment = fetchAssignmentById(submission.getAssignmentId());

        // Yetkilendirme yoxlaması
        boolean isTeacher = assignment.getTeacherId().equals(userId);
        boolean isOwner = submission.getStudentId().equals(userId);

        if (!isTeacher && !isOwner) {
            throw new UnauthorizedException("You are not authorized to download this file");
        }

        if (submission.getFileName() == null) {
            throw new ResourceNotFoundException("No file attached to this submission");
        }

        return minioService.downloadFile(submission.getFileName());
    }

    // ─── GET SUBMISSION FILE NAME ───
    @Transactional(readOnly = true)
    public String getSubmissionFileName(Long submissionId) {
        AssignmentSubmission submission = fetchSubmissionById(submissionId);
        if (submission.getFileName() == null) {
            return "submission_" + submissionId;
        }
        // Original file name-i qaytar (MinIO-dakı yolun son hissəsi)
        String fileName = submission.getFileName();
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        return fileName;
    }

    // ─── GET SUBMISSION CONTENT TYPE ───
    @Transactional(readOnly = true)
    public String getSubmissionContentType(Long submissionId) {
        AssignmentSubmission submission = fetchSubmissionById(submissionId);
        return submission.getMimeType() != null ? submission.getMimeType() : "application/octet-stream";
    }

    // ─── CHECK IF SUBMISSION HAS FILE ───
    @Transactional(readOnly = true)
    public boolean submissionHasFile(Long submissionId) {
        AssignmentSubmission submission = fetchSubmissionById(submissionId);
        return submission.getFileName() != null && !submission.getFileName().isEmpty();
    }

    // ─── GET SUBMISSION BY ID (with authorization) ───
    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getSubmissionById(Long submissionId, Long userId) {
        AssignmentSubmission submission = fetchSubmissionById(submissionId);
        Assignment assignment = fetchAssignmentById(submission.getAssignmentId());

        boolean isTeacher = assignment.getTeacherId().equals(userId);
        boolean isOwner = submission.getStudentId().equals(userId);

        if (!isTeacher && !isOwner) {
            throw new UnauthorizedException("You are not authorized to view this submission");
        }

        return assignmentMapper.toDto(submission);
    }

    // ─── HELPERS ───
    private void validateOwnership(Assignment assignment, Long teacherId) {
        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to modify this assignment");
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