package com.Classroom_ai.Classroom.Cours;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Setter
@Getter
@Entity
@Table(name = "courses")
public class Course {
    // Getters and Setters
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

}
