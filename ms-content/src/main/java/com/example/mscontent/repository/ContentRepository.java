package com.example.mscontent.repository;

import com.example.mscontent.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByCourseId(Long courseId);
    List<Content> findByTeacherId(Long teacherId);
}