package com.Classroom_ai.Classroom.Cours;

import com.Classroom_ai.Classroom.User.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5175")  // CORS for React app
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCourse(@RequestBody @Valid Course course) {
        try {
            Course createdCourse = courseService.createCourse(course);
            return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur interne du serveur.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
