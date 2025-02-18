package com.Classroom_ai.Classroom.Cours;

import com.Classroom_ai.Classroom.CourseFile.CourseFile;
import com.Classroom_ai.Classroom.User.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Course Name is required")
    @Column(nullable = false)
    private String courseName;

    @NotBlank(message = "Section is required")
    @Column(nullable = false)
    private String section;

    @NotBlank(message = "Subject is required")
    @Column(nullable = false)
    private String subject;

    @NotNull(message = "Room is required")
    @Column(nullable = false)
    @Min(value = 1, message = "Room must be greater than 0")
    private Integer room;

    private String accessCode;
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseFile> files = new ArrayList<>();


    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnoreProperties({"password", "courses"})  // Avoid unnecessary fields like password
    private User teacher;  // Teacher or creator of the course


    @ManyToMany(mappedBy = "courses")
    @JsonIgnore
    private List<User> students; // List of students enrolled in the course

    protected String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
