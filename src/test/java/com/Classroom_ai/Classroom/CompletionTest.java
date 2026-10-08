package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.course.CourseRepository;
import com.Classroom_ai.Classroom.generation.ExerciseSet;
import com.Classroom_ai.Classroom.generation.ExerciseSetRepository;
import com.Classroom_ai.Classroom.generation.Status;
import com.Classroom_ai.Classroom.generation.Summary;
import com.Classroom_ai.Classroom.generation.SummaryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S6 Completion: delete a file, delete a course, leave a course, list a course's students.
 * Per Target API row: the allowed role succeeds, a signed-in non-member gets 403, a signed-out caller gets 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CompletionTest {

    private static final String PASSWORD = "secret-pw";
    private static final String PDF = "%PDF-1.4 completion";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Autowired CourseRepository courses;
    @Autowired CourseFileRepository files;
    @Autowired SummaryRepository summaries;
    @Autowired ExerciseSetRepository exerciseSets;
    @Value("${upload.dir}") String uploadDir;

    @BeforeAll
    static void createUploadDir(@Value("${upload.dir}") String dir) throws Exception {
        Files.createDirectories(Paths.get(dir));
    }

    // --- DELETE /api/files/{id} (teacher) ---

    @Test
    void deleteFile_byTeacher_is204AndTheFileIsGone() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = idOf(createCourse(teacher, "Del-" + UUID.randomUUID()));
        String fileId = uploadPdf(teacher, courseId);
        Path stored = storedPath(fileId);
        assertTrue(Files.exists(stored));

        mvc.perform(delete("/api/files/" + fileId).header("Authorization", teacher))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", teacher))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/courses/" + courseId + "/files").header("Authorization", teacher))
                .andExpect(jsonPath("$", hasSize(0)));
        assertFalse(Files.exists(stored), "the stored PDF is removed from disk");
    }

    @Test
    void deleteFile_removesItsSummariesAndExerciseSets() throws Exception {
        User teacherUser = newUser("Teach");
        String teacher = bearer(teacherUser);
        String courseId = idOf(createCourse(teacher, "Gen-" + UUID.randomUUID()));
        String fileId = uploadPdf(teacher, courseId);
        CourseFile file = files.findById(Long.valueOf(fileId)).orElseThrow();
        long summariesBefore = summaries.count();
        long setsBefore = exerciseSets.count();
        summaries.save(summary(file, teacherUser));
        exerciseSets.save(exerciseSet(file, teacherUser));

        mvc.perform(delete("/api/files/" + fileId).header("Authorization", teacher))
                .andExpect(status().isNoContent());

        assertEquals(summariesBefore, summaries.count());
        assertEquals(setsBefore, exerciseSets.count());
    }

    @Test
    void deleteFile_byStudent_is403AndKeepsTheFile() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Keep-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        String fileId = uploadPdf(teacher, idOf(course));

        mvc.perform(delete("/api/files/" + fileId).header("Authorization", student))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", teacher))
                .andExpect(status().isOk());
    }

    @Test
    void deleteFile_byNonMember_is403() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String fileId = uploadPdf(teacher, idOf(createCourse(teacher, "Out-" + UUID.randomUUID())));

        mvc.perform(delete("/api/files/" + fileId).header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFile_signedOut_is401() throws Exception {
        mvc.perform(delete("/api/files/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteFile_missing_is404() throws Exception {
        mvc.perform(delete("/api/files/999999").header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    // --- DELETE /api/courses/{id} (teacher) ---

    @Test
    void deleteCourse_byTeacher_is204AndTheCourseIsGoneForEveryone() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Gone-" + UUID.randomUUID());
        String courseId = idOf(course);
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        String fileId = uploadPdf(teacher, courseId);
        Path stored = storedPath(fileId);

        mvc.perform(delete("/api/courses/" + courseId).header("Authorization", teacher))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/courses/" + courseId).header("Authorization", teacher))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/courses").header("Authorization", teacher))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", teacher))
                .andExpect(status().isNotFound());
        assertFalse(Files.exists(stored), "the stored PDF is removed from disk");
    }

    @Test
    void deleteCourse_removesTheGeneratedItemsOfItsFiles() throws Exception {
        User teacherUser = newUser("Teach");
        String teacher = bearer(teacherUser);
        String courseId = idOf(createCourse(teacher, "GenGone-" + UUID.randomUUID()));
        CourseFile file = files.findById(Long.valueOf(uploadPdf(teacher, courseId))).orElseThrow();
        long summariesBefore = summaries.count();
        long setsBefore = exerciseSets.count();
        summaries.save(summary(file, teacherUser));
        exerciseSets.save(exerciseSet(file, teacherUser));

        mvc.perform(delete("/api/courses/" + courseId).header("Authorization", teacher))
                .andExpect(status().isNoContent());

        assertEquals(summariesBefore, summaries.count());
        assertEquals(setsBefore, exerciseSets.count());
    }

    @Test
    void deleteCourse_byStudent_is403AndKeepsTheCourse() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Mine-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(delete("/api/courses/" + idOf(course)).header("Authorization", student))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/courses/" + idOf(course)).header("Authorization", teacher))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCourse_byNonMember_is403() throws Exception {
        String course = createCourse(bearer(newUser("Teach")), "Safe-" + UUID.randomUUID());

        mvc.perform(delete("/api/courses/" + idOf(course)).header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCourse_signedOut_is401() throws Exception {
        mvc.perform(delete("/api/courses/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCourse_missing_is404() throws Exception {
        mvc.perform(delete("/api/courses/999999").header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    // --- DELETE /api/courses/{id}/membership (student) ---

    @Test
    void leave_byStudent_is204AndTheCourseLeavesTheirList() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Leave-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(delete("/api/courses/" + idOf(course) + "/membership").header("Authorization", student))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/courses/" + idOf(course)).header("Authorization", student))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/" + idOf(course)).header("Authorization", teacher))
                .andExpect(status().isOk());
    }

    @Test
    void leave_thenJoinAgain_works() throws Exception {
        String student = bearer(newUser("Stud"));
        String course = createCourse(bearer(newUser("Teach")), "Back-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        mvc.perform(delete("/api/courses/" + idOf(course) + "/membership").header("Authorization", student))
                .andExpect(status().isNoContent());

        join(student, accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void leave_byTeacher_is403() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String course = createCourse(teacher, "Stay-" + UUID.randomUUID());

        mvc.perform(delete("/api/courses/" + idOf(course) + "/membership").header("Authorization", teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void leave_byNonMember_is403() throws Exception {
        String course = createCourse(bearer(newUser("Teach")), "NotIn-" + UUID.randomUUID());

        mvc.perform(delete("/api/courses/" + idOf(course) + "/membership").header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void leave_signedOut_is401() throws Exception {
        mvc.perform(delete("/api/courses/1/membership")).andExpect(status().isUnauthorized());
    }

    @Test
    void leave_missingCourse_is404() throws Exception {
        mvc.perform(delete("/api/courses/999999/membership").header("Authorization", bearer(newUser("Stud"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    // --- GET /api/courses/{id}/students (teacher) ---

    @Test
    void students_byTeacher_listsTheEnrolledStudentsWithoutSecrets() throws Exception {
        String teacher = bearer(newUser("Teach"));
        User first = newUser("Ann");
        User second = newUser("Ben");
        String course = createCourse(teacher, "Roster-" + UUID.randomUUID());
        join(bearer(first), accessCodeOf(course)).andExpect(status().isOk());
        join(bearer(second), accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(get("/api/courses/" + idOf(course) + "/students").header("Authorization", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].firstName").value(containsInAnyOrder("Ann", "Ben")))
                .andExpect(jsonPath("$[*].email").value(containsInAnyOrder(first.getEmail(), second.getEmail())))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void students_ofACourseWithNoStudents_isEmpty() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String course = createCourse(teacher, "Empty-" + UUID.randomUUID());

        mvc.perform(get("/api/courses/" + idOf(course) + "/students").header("Authorization", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void students_byStudent_is403() throws Exception {
        String student = bearer(newUser("Stud"));
        String course = createCourse(bearer(newUser("Teach")), "Peek-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(get("/api/courses/" + idOf(course) + "/students").header("Authorization", student))
                .andExpect(status().isForbidden());
    }

    @Test
    void students_byNonMember_is403() throws Exception {
        String course = createCourse(bearer(newUser("Teach")), "Closed-" + UUID.randomUUID());

        mvc.perform(get("/api/courses/" + idOf(course) + "/students").header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void students_signedOut_is401() throws Exception {
        mvc.perform(get("/api/courses/1/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void students_missingCourse_is404() throws Exception {
        mvc.perform(get("/api/courses/999999/students").header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    // --- helpers ---

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", UUID.randomUUID() + "@example.com",
                encoder.encode(PASSWORD), null));
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    private static String courseJson(String name) {
        return "{\"courseName\":\"" + name + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}";
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "notes.pdf", "application/pdf", PDF.getBytes(StandardCharsets.UTF_8));
    }

    /** Creates a course as the user behind {@code auth} and returns the response body. */
    private String createCourse(String auth, String name) throws Exception {
        return mvc.perform(post("/api/courses").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(courseJson(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions join(String auth, String code) throws Exception {
        return mvc.perform(post("/api/courses/join").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"accessCode\":\"" + code + "\"}"));
    }

    /** Uploads a PDF to the course as its teacher and returns the new file's id. */
    private String uploadPdf(String teacherAuth, String courseId) throws Exception {
        String body = mvc.perform(multipart("/api/courses/" + courseId + "/files").file(pdf())
                        .header("Authorization", teacherAuth))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id").toString();
    }

    private Path storedPath(String fileId) {
        CourseFile file = files.findById(Long.valueOf(fileId)).orElseThrow();
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(file.getFilePath());
    }

    private static Summary summary(CourseFile file, User author) {
        Summary summary = new Summary();
        summary.setFile(file);
        summary.setAuthor(author);
        summary.setPublished(true);
        summary.setStatus(Status.DONE);
        summary.setContent("A summary.");
        return summary;
    }

    private static ExerciseSet exerciseSet(CourseFile file, User author) {
        ExerciseSet set = new ExerciseSet();
        set.setFile(file);
        set.setAuthor(author);
        set.setPublished(true);
        set.setStatus(Status.DONE);
        set.setExercisesJson("[]");
        return set;
    }

    private static String idOf(String courseJson) {
        return JsonPath.read(courseJson, "$.id").toString();
    }

    private static String accessCodeOf(String courseJson) {
        return JsonPath.read(courseJson, "$.accessCode");
    }
}
