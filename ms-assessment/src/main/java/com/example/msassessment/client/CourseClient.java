package com.example.msassessment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-course", url = "${MS_COURSE_URL:http://localhost:8082}")
public interface CourseClient {

    @GetMapping("/api/courses/{id}")
    CourseResponse getCourseById(@PathVariable Long id);
}