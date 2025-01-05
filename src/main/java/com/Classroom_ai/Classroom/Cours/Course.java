package com.Classroom_ai.Classroom.Cours;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "courses")
public class Course {
    // Getters and Setters
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
    private String room;

}
