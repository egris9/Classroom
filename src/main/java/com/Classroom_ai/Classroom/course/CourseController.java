package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.api.CourseRequest;
import com.Classroom_ai.Classroom.api.CourseResponse;
import com.Classroom_ai.Classroom.api.FileResponse;
import com.Classroom_ai.Classroom.api.JoinRequest;
import com.Classroom_ai.Classroom.api.UserResponse;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.membership.Role;
import jakarta.validation.Valid;
import com.Classroom_ai.Classroom.material.Materials;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {
    private final CourseService courseService;
    private final CourseFileService fileService;
    private final UserService userService;
    private final Materials materials;

    public CourseController(CourseService courseService, CourseFileService fileService, UserService userService,
                            Materials materials) {
        this.courseService = courseService;
        this.fileService = fileService;
        this.userService = userService;
        this.materials = materials;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody @Valid CourseRequest request) {
        return respond(courseService.create(userService.getAuthenticatedUser(), request));
    }

    @GetMapping("/courses")
    public List<CourseResponse> list() {
        return courseService.listFor(userService.getAuthenticatedUser()).stream().map(CourseController::respond).toList();
    }

    @PostMapping("/courses/join")
    public CourseResponse join(@RequestBody @Valid JoinRequest request) {
        return respond(courseService.join(userService.getAuthenticatedUser(), request.accessCode()));
    }

    @GetMapping("/courses/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return respond(courseService.getFor(userService.getAuthenticatedUser(), id));
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(userService.getAuthenticatedUser(), id);
    }

    @DeleteMapping("/courses/{id}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id) {
        courseService.leave(userService.getAuthenticatedUser(), id);
    }

    @GetMapping("/courses/{id}/students")
    public List<UserResponse> students(@PathVariable Long id) {
        return courseService.studentsOf(userService.getAuthenticatedUser(), id).stream().map(UserResponse::of).toList();
    }

    @PostMapping("/courses/{id}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return respond(fileService.add(userService.getAuthenticatedUser(), id, file));
    }

    @GetMapping("/courses/{id}/files")
    public List<FileResponse> files(@PathVariable Long id) {
        return fileService.list(userService.getAuthenticatedUser(), id).stream().map(CourseController::respond).toList();
    }

    @DeleteMapping("/files/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable Long id) {
        fileService.delete(userService.getAuthenticatedUser(), id);
    }

    @GetMapping("/files/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) throws IOException {
        CourseFile file = fileService.get(userService.getAuthenticatedUser(), id);
        Resource pdf = materials.open(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.getFileName()).build().toString())
                .contentLength(pdf.contentLength())
                .body(pdf);
    }

    private static CourseResponse respond(CourseAccess access) {
        Course course = access.course();
        User teacher = course.getTeacher();
        return new CourseResponse(course.getId(), course.getCourseName(), course.getSection(), course.getSubject(),
                course.getRoom(), new CourseResponse.TeacherResponse(teacher.getId(), teacher.getFirstName(), UserResponse.pictureUrl(teacher)),
                access.role(), access.role() == Role.TEACHER ? course.getAccessCode() : null);
    }

    private static FileResponse respond(CourseFile file) {
        return new FileResponse(file.getId(), file.getFileName());
    }
}
