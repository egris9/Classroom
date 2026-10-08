package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.course.CourseFile;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** What a Summary and an Exercise set share: whose it is, who may see it, how the job went, and which model wrote it. */
@Getter
@Setter
@MappedSuperclass
public abstract class Generated {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private CourseFile file;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private boolean published;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String model;
    private String failureCode;
    private String failureMessage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
