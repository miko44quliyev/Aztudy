package com.example.mscourse.repository;

import com.example.mscourse.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course,Long> {

    List<Course> findByTeacherId(Long teacherId);

    Optional<Course> findByCourseCode(String courseCode);

}
