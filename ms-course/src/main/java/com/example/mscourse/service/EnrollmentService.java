package com.example.mscourse.service;

import com.example.mscourse.client.UserClient;
import com.example.mscourse.entity.Course;
import com.example.mscourse.entity.Enrollment;
import com.example.mscourse.exception.ResourceNotFoundException;
import com.example.mscourse.exception.UnauthorizedException;
import com.example.mscourse.repository.CourseRepository;
import com.example.mscourse.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserClient userClient;

    @CacheEvict(value = {"enrollments_by_student", "enrollments_by_course"}, allEntries = true)
    @Transactional
    public Enrollment enrollByCode(String courseCode, Long studentId) {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with code: " + courseCode));

        if (enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), studentId)) {
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(course.getId());
        enrollment.setStudentId(studentId);
        enrollment.setStatus("ACTIVE");
        return enrollmentRepository.save(enrollment);
    }
    @CacheEvict(value = {"enrollments_by_student", "enrollments_by_course"}, allEntries = true)
    @Transactional
    public Enrollment enrollStudentByEmail(Long courseId, String studentEmail, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (!course.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to manage this course");
        }

        Long studentId = userClient.getUserIdByEmail(studentEmail);

        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new IllegalStateException("Student is already enrolled");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus("ACTIVE");
        return enrollmentRepository.save(enrollment);
    }
    @CacheEvict(value = {"enrollments_by_student", "enrollments_by_course"}, allEntries = true)
    @Transactional
    public void withdrawEnrollment(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollmentRepository.delete(enrollment);
    }
    @Cacheable(value = "enrollments_by_student", key = "#studentId")
    public List<Enrollment> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }
    @Cacheable(value = "enrollments_by_course", key = "#courseId")
    public List<Enrollment> getCourseEnrollments(Long courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        return enrollmentRepository.findByCourseId(courseId);
    }
}