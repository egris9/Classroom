package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.api.CourseResponse;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredPicture;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** The cover picture of a course. Reading is open to anyone, because an {@code <img>} tag cannot send a token. */
@RestController
@RequestMapping("/api/courses/{id}/picture")
public class CoursePictureController {
    private final CourseService courseService;
    private final UserService userService;
    private final Materials materials;

    public CoursePictureController(CourseService courseService, UserService userService, Materials materials) {
        this.courseService = courseService;
        this.userService = userService;
        this.materials = materials;
    }

    @PutMapping
    public CourseResponse set(@PathVariable Long id, @RequestParam("picture") MultipartFile picture) throws IOException {
        return CourseController.respond(courseService.setPicture(userService.getAuthenticatedUser(), id, picture));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        courseService.removePicture(userService.getAuthenticatedUser(), id);
    }

    @GetMapping
    public ResponseEntity<Resource> get(@PathVariable Long id) {
        Course course = courseService.find(id);
        StoredPicture picture = materials.openPicture(course.getPicturePath());
        return ResponseEntity.ok().contentType(picture.type()).body(picture.resource());
    }
}
