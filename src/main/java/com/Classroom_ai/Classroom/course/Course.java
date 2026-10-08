package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private Integer room;

    private String accessCode;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseFile> files = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany(mappedBy = "courses")
    private List<User> students = new ArrayList<>();

    static String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
