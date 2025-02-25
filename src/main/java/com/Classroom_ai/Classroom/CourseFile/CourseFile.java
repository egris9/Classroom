package com.Classroom_ai.Classroom.CourseFile;

import com.Classroom_ai.Classroom.Cours.Course;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_files")
public class CourseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;  // Nom du fichier PDF

    @Column(nullable = false)
    private String filePath;  // Emplacement du fichier PDF

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;
}
