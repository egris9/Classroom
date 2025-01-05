package com.Classroom_ai.Classroom.Cours;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/courses")
@CrossOrigin // Pour autoriser les requêtes depuis votre frontend
public class CourseController {
    private CourseService courseService;

    @PostMapping("/create")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course createdCourse = courseService.createCourse(course);
        return ResponseEntity.ok(createdCourse);
    }

    @GetMapping("/join/{id}")
    public ResponseEntity<Course> joinCourse(@PathVariable Long id) {
        Optional<Course> course = courseService.joinCourse(id);
        return course.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
