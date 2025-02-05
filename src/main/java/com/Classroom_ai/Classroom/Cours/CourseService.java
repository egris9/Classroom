package com.Classroom_ai.Classroom.Cours;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service

public class CourseService {
    private final CourseRepository courseRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Course createCourse(Course course) {
        // Vérifier si le cours existe déjà
        if (courseRepository.findByCourseName(course.getCourseName()).isPresent()) {
            throw new CourseAlreadyExistsException("A course with this name already exists");
        }
        return courseRepository.save(course);
    }

    public Optional<Course> joinCourseByAccessCode(String accessCode) {
        System.out.println("Requête avec accessCode: " + accessCode); // Log de l'accessCode
        return courseRepository.findByAccessCode(accessCode);
    }
}
