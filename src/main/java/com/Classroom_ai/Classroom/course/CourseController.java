package com.Classroom_ai.Classroom.course;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.api.CourseRequest;
import com.Classroom_ai.Classroom.api.CourseResponse;
import com.Classroom_ai.Classroom.api.FileResponse;
import com.Classroom_ai.Classroom.api.JoinRequest;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.membership.Role;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {
    private final CourseService courseService;
    private final CourseFileService fileService;
    private final UserService userService;

    public CourseController(CourseService courseService, CourseFileService fileService, UserService userService) {
        this.courseService = courseService;
        this.fileService = fileService;
        this.userService = userService;
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

    @PostMapping("/courses/{id}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return respond(fileService.add(userService.getAuthenticatedUser(), id, file));
    }

    @GetMapping("/courses/{id}/files")
    public List<FileResponse> files(@PathVariable Long id) {
        return fileService.list(userService.getAuthenticatedUser(), id).stream().map(CourseController::respond).toList();
    }

    @GetMapping("/files/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) throws IOException {
        CourseFile file = fileService.get(userService.getAuthenticatedUser(), id);
        Path path = Paths.get(file.getFilePath());
        if (!Files.isReadable(path)) {
            throw new CourseFileNotFoundException("The stored file is missing.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.getFileName()).build().toString())
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }

    private static CourseResponse respond(CourseAccess access) {
        Course course = access.course();
        User teacher = course.getTeacher();
        return new CourseResponse(course.getId(), course.getCourseName(), course.getSection(), course.getSubject(),
                course.getRoom(), new CourseResponse.TeacherResponse(teacher.getFirstName(), teacher.getProfilePicturePath()),
                access.role(), access.role() == Role.TEACHER ? course.getAccessCode() : null);
    }

    private static FileResponse respond(CourseFile file) {
        return new FileResponse(file.getId(), file.getFileName());
    }
}
