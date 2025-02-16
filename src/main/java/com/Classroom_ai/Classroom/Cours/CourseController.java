package com.Classroom_ai.Classroom.Cours;
import com.Classroom_ai.Classroom.User.User;

import com.Classroom_ai.Classroom.User.UserService;
import io.micrometer.common.lang.NonNull;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5175")  // CORS for React app
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;



    public CourseController(CourseService courseService,UserService userService) {

        this.courseService = courseService;
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCourse(@NonNull @RequestBody @Valid Course course) {
        try {
            // Retrieve the current authenticated user
            User currentUser = userService.getAuthenticatedUser();

            // Set the current user as the teacher of the course
            course.setTeacher(currentUser);

            // Create the course
            Course createdCourse = courseService.createCourse(course);
            return new ResponseEntity<>(Map.of("accessCode", createdCourse.getAccessCode()), HttpStatus.CREATED);
        } catch (CourseAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur interne du serveur.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/join")
    public ResponseEntity<?> joinCourse(@RequestParam String accessCode) {
        try {
            Course course = courseService.joinCourseByAccessCode(accessCode);
            return new ResponseEntity<>(course, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }





    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(@RequestParam String type) {
        try {
            User currentUser = userService.getAuthenticatedUser();

            List<Course> createdCourses = courseService.getCoursesCreatedByUser(currentUser.getId());
            List<Course> joinedCourses = courseService.getCoursesJoinedByUser(currentUser.getId());

            // Map both lists to the response format with "type" (created or joined)
            List<Map<String, Object>> createdCoursesResponse = mapCoursesToResponse(createdCourses, "created");
            List<Map<String, Object>> joinedCoursesResponse = mapCoursesToResponse(joinedCourses, "joined");

            // Combine both lists
            List<Map<String, Object>> coursesResponse = Stream.concat(
                    createdCoursesResponse.stream(),
                    joinedCoursesResponse.stream()
            ).collect(Collectors.toList());

            return ResponseEntity.ok(coursesResponse);
        } catch (Exception e) {
            return new ResponseEntity<>("Error fetching courses: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    private List<Map<String, Object>> mapCoursesToResponse(List<Course> courses, String type) {
        return courses.stream().map(course -> {
            Map<String, Object> courseData = new HashMap<>();
            courseData.put("id", course.getId());
            courseData.put("name", course.getCourseName());
            User teacher = course.getTeacher();
            if (teacher != null) {
                Map<String, String> teacherData = new HashMap<>();
                teacherData.put("name", teacher.getFirstName());
                teacherData.put("profilePicture", teacher.getProfilePicturePath());
                courseData.put("teacher", teacherData);
            } else {
                courseData.put("teacher", null);
            }
            courseData.put("type", type);
            return courseData;
        }).collect(Collectors.toList());
    }














}
