package com.example.mscourse.service;

import com.example.mscourse.dto.CourseRequest;
import com.example.mscourse.dto.CourseResponse;
import com.example.mscourse.dto.CourseUpdateRequest;
import com.example.mscourse.entity.Course;
import com.example.mscourse.exception.ResourceNotFoundException;
import com.example.mscourse.exception.UnauthorizedException;
import com.example.mscourse.mapper.CourseMapper;
import com.example.mscourse.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    // ─── internal entity fetch ───
    private Course fetchCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    // ─── CREATE ───
    @CacheEvict(value = {"courses", "courses_by_teacher"}, allEntries = true)
    @Transactional
    public CourseResponse createCourse(CourseRequest request, Long teacherId) {
        Course course = courseMapper.fromDto(request);
        course.setTeacherId(teacherId);
        course.setCourseCode(generateCourseCode());
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        return courseMapper.toDto(fetchCourseById(id));
    }

    // ─── UPDATE FULLY ───
    @CacheEvict(value = {"courses", "courses_by_teacher"}, allEntries = true)
    @Transactional
    public CourseResponse updateCourseFully(Long id, CourseUpdateRequest request, Long teacherId) {
        Course course = fetchCourseById(id);
        validateOwnership(course, teacherId);
        courseMapper.updateFromDtoWithNull(request, course);
        return courseMapper.toDto(courseRepository.save(course));
    }

    // ─── UPDATE PARTIAL ───
    @CacheEvict(value = {"courses", "courses_by_teacher"}, allEntries = true)
    @Transactional
    public CourseResponse updateCourse(Long id, CourseUpdateRequest request, Long teacherId) {
        Course course = fetchCourseById(id);
        validateOwnership(course, teacherId);
        courseMapper.updateFromDto(request, course);
        return courseMapper.toDto(courseRepository.save(course));
    }

    // ─── DELETE ───
    @CacheEvict(value = {"courses", "courses_by_teacher"}, allEntries = true)
    @Transactional
    public void deleteCourse(Long id, Long teacherId) {
        Course course = fetchCourseById(id);
        validateOwnership(course, teacherId);
        courseRepository.delete(course);
    }

    // ─── GET ALL ───
    @Cacheable(value = "courses_by_teacher", key = "#teacherId")
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId)
                .stream()
                .map(courseMapper::toDto)
                .toList();
    }

    // ─── HELPERS ───
    private void validateOwnership(Course course, Long teacherId) {
        if (!course.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("You are not authorized to modify this course");
        }
    }

    private String generateCourseCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}