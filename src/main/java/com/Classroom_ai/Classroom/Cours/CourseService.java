package com.Classroom_ai.Classroom.Cours;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service

public class CourseService {
    private CourseRepository courseRepository;

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public Optional<Course> joinCourse(Long id) {
        return courseRepository.findById(id);
    }
}
