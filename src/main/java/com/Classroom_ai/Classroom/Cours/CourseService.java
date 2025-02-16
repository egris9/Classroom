package com.Classroom_ai.Classroom.Cours;
import com.Classroom_ai.Classroom.User.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository; // Make sure this is declared
    private final UserService userService;

    @Autowired
    public CourseService(CourseRepository courseRepository, UserRepository userRepository,UserService userService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;  // Ensure the UserRepository is injected here
        this.userService = userService;

    }

    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleException(Exception e) {
            logger.error("Internal Server Error occurred: {}", e.getMessage(), e); // Log the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur.");
        }
    }



    public Course createCourse(Course course) {
        // Check for duplicate course name
        if (courseRepository.findByCourseName(course.getCourseName()).isPresent()) {
            throw new CourseAlreadyExistsException("A course with this name already exists");
        }

        // Get the authenticated user and set as the teacher
        User authenticatedUser = userService.getAuthenticatedUser();
        course.setTeacher(authenticatedUser);

        // Generate the access code
        course.setAccessCode(course.generateAccessCode());

        // Save the course
        return courseRepository.save(course);
    }



    public Course joinCourseByAccessCode(String accessCode) {
        Course course = courseRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with access code: " + accessCode));

        User authenticatedUser = userService.getAuthenticatedUser();
        System.out.println("Authenticated User: " + authenticatedUser.getId());

        // Check if the user is already in the course
        if (!course.getStudents().contains(authenticatedUser)) {
            System.out.println("User not already enrolled, adding user to course.");
            course.getStudents().add(authenticatedUser);
            authenticatedUser.getCourses().add(course);
            courseRepository.save(course);  // Save the updated course
            userRepository.save(authenticatedUser);  // Save the updated user
        } else {
            System.out.println("User already enrolled in the course.");
        }

        return course;
    }




   /* public Optional<Course> joinCourseByAccessCode(String accessCode) {
        System.out.println("Requête avec accessCode: " + accessCode); // Log de l'accessCode
        return courseRepository.findByAccessCode(accessCode);
    }*/







    public List<Course> getCoursesCreatedByUser(Long userId) {
        return courseRepository.findByTeacherId(userId);
    }

    // Get courses that the user has joined
    public List<Course> getCoursesJoinedByUser(Long userId) {
        return courseRepository.findByStudentsContaining(userService.getAuthenticatedUser());
    }




}


