package com.example.mscourse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long studentId;

    @Column(updatable = false)
    private LocalDateTime enrolledAt;

    private String status = "ACTIVE"; // ACTIVE, COMPLETED, DROPPED

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }
}