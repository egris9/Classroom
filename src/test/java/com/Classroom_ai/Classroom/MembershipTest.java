package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S2 Membership: every course and file endpoint checks the caller's role in that course.
 * Per Target API row: the allowed role succeeds, a signed-in non-member gets 403,
 * a signed-out caller gets 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipTest {

    private static final String PASSWORD = "secret-pw";
    private static final String PDF = "%PDF-1.4 membership";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Autowired CourseFileRepository files;

    @BeforeAll
    static void createUploadDir(@Value("${upload.dir}") String dir) throws Exception {
        Files.createDirectories(Paths.get(dir));
    }

    // --- POST /api/courses (signed in) ---

    @Test
    void createCourse_returnsCourseWithTeacherRoleAndAccessCode() throws Exception {
        mvc.perform(post("/api/courses").header("Authorization", bearer(newUser("Teach")))
                        .contentType(MediaType.APPLICATION_JSON).content(courseJson("Math-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.accessCode").value(notNullValue()))
                .andExpect(jsonPath("$.teacher.password").doesNotExist());
    }

    @Test
    void createCourse_signedOut_is401() throws Exception {
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(courseJson("X")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCourse_sameNameTwiceForOneTeacher_is409() throws Exception {
        String auth = bearer(newUser("Teach"));
        String name = "Dup-" + UUID.randomUUID();
        createCourse(auth, name);

        mvc.perform(post("/api/courses").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(courseJson(name)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_NAME_TAKEN"));
    }

    @Test
    void createCourse_sameNameForTwoTeachers_isAllowed() throws Exception {
        String name = "Shared-" + UUID.randomUUID();
        createCourse(bearer(newUser("TeachA")), name);

        mvc.perform(post("/api/courses").header("Authorization", bearer(newUser("TeachB")))
                        .contentType(MediaType.APPLICATION_JSON).content(courseJson(name)))
                .andExpect(status().isCreated());
    }

    // --- GET /api/courses (signed in) ---

    @Test
    void listCourses_carriesTheCallersRolePerCourse() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String code = accessCodeOf(createCourse(teacher, "Own-" + UUID.randomUUID()));
        join(student, code).andExpect(status().isOk());

        mvc.perform(get("/api/courses").header("Authorization", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("TEACHER"));
        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].accessCode").value(nullValue()));
    }

    @Test
    void listCourses_signedOut_is401() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    // --- POST /api/courses/join (signed in) ---

    @Test
    void join_enrolsTheCallerAsStudent() throws Exception {
        String code = accessCodeOf(createCourse(bearer(newUser("Teach")), "Join-" + UUID.randomUUID()));

        join(bearer(newUser("Stud")), code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void join_twice_keepsOneEnrolment() throws Exception {
        String student = bearer(newUser("Stud"));
        String code = accessCodeOf(createCourse(bearer(newUser("Teach")), "Twice-" + UUID.randomUUID()));
        join(student, code).andExpect(status().isOk());
        join(student, code).andExpect(status().isOk());

        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void join_yourOwnCourse_is409() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String code = accessCodeOf(createCourse(teacher, "Mine-" + UUID.randomUUID()));

        join(teacher, code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_TEACHER"));
    }

    @Test
    void join_unknownCode_is404() throws Exception {
        join(bearer(newUser("Stud")), "NOSUCH00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    void join_signedOut_is401() throws Exception {
        mvc.perform(post("/api/courses/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"ABCDEFGH\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void join_byGet_doesNotEnrol() throws Exception {
        String student = bearer(newUser("Stud"));
        String code = accessCodeOf(createCourse(bearer(newUser("Teach")), "NoGet-" + UUID.randomUUID()));

        mvc.perform(get("/api/courses/join").param("accessCode", code).header("Authorization", student));

        mvc.perform(get("/api/courses").header("Authorization", student))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/courses/{id} (member) ---

    @Test
    void getCourse_teacherAndStudentSucceed() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Get-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        String id = idOf(course);

        mvc.perform(get("/api/courses/" + id).header("Authorization", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.accessCode").value(notNullValue()));
        mvc.perform(get("/api/courses/" + id).header("Authorization", student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void getCourse_nonMember_is403() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "Priv-" + UUID.randomUUID()));

        mvc.perform(get("/api/courses/" + id).header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getCourse_signedOut_is401() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "Out-" + UUID.randomUUID()));

        mvc.perform(get("/api/courses/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void getCourse_missing_is404() throws Exception {
        mvc.perform(get("/api/courses/999999").header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    // --- POST /api/courses/{id}/files (teacher) ---

    @Test
    void uploadFile_teacherSucceeds() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String id = idOf(createCourse(teacher, "Up-" + UUID.randomUUID()));

        mvc.perform(multipart("/api/courses/" + id + "/files").file(pdf()).header("Authorization", teacher))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.fileName").value("notes.pdf"))
                .andExpect(jsonPath("$.filePath").doesNotExist());
    }

    @Test
    void uploadFile_student_is403() throws Exception {
        String student = bearer(newUser("Stud"));
        String course = createCourse(bearer(newUser("Teach")), "UpS-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());

        mvc.perform(multipart("/api/courses/" + idOf(course) + "/files").file(pdf()).header("Authorization", student))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadFile_nonMember_is403() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "UpN-" + UUID.randomUUID()));

        mvc.perform(multipart("/api/courses/" + id + "/files").file(pdf()).header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadFile_signedOut_is401() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "UpO-" + UUID.randomUUID()));

        mvc.perform(multipart("/api/courses/" + id + "/files").file(pdf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadFile_missingCourse_is404() throws Exception {
        mvc.perform(multipart("/api/courses/999999/files").file(pdf()).header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/courses/{id}/files (member) ---

    @Test
    void listFiles_teacherAndStudentSucceed() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "List-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        uploadPdf(teacher, idOf(course));

        for (String auth : new String[]{teacher, student}) {
            mvc.perform(get("/api/courses/" + idOf(course) + "/files").header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fileName").value("notes.pdf"))
                    .andExpect(jsonPath("$[0].filePath").doesNotExist());
        }
    }

    @Test
    void listFiles_nonMember_is403() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "LsN-" + UUID.randomUUID()));

        mvc.perform(get("/api/courses/" + id + "/files").header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFiles_signedOut_is401() throws Exception {
        String id = idOf(createCourse(bearer(newUser("Teach")), "LsO-" + UUID.randomUUID()));

        mvc.perform(get("/api/courses/" + id + "/files")).andExpect(status().isUnauthorized());
    }

    // --- GET /api/files/{id}/content (member) ---

    @Test
    void fileContent_teacherAndStudentGetThePdf() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String student = bearer(newUser("Stud"));
        String course = createCourse(teacher, "Cnt-" + UUID.randomUUID());
        join(student, accessCodeOf(course)).andExpect(status().isOk());
        String fileId = uploadPdf(teacher, idOf(course));

        for (String auth : new String[]{teacher, student}) {
            MvcResult result = mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andReturn();
            assertEquals(PDF, result.getResponse().getContentAsString());
        }
    }

    @Test
    void fileContent_nonMember_is403() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String fileId = uploadPdf(teacher, idOf(createCourse(teacher, "CnN-" + UUID.randomUUID())));

        mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", bearer(newUser("Stranger"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fileContent_signedOut_is401() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String fileId = uploadPdf(teacher, idOf(createCourse(teacher, "CnO-" + UUID.randomUUID())));

        mvc.perform(get("/api/files/" + fileId + "/content")).andExpect(status().isUnauthorized());
    }

    @Test
    void fileContent_missingFile_is404() throws Exception {
        mvc.perform(get("/api/files/999999/content").header("Authorization", bearer(newUser("Teach"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void fileContent_quoteInFileName_isEscapedInContentDisposition() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = idOf(createCourse(teacher, "Quote-" + UUID.randomUUID()));
        String fileId = uploadPdf(teacher, courseId);
        // A display name can hold a quote even where the disk cannot (Windows), so set it on the record.
        CourseFile stored = files.findById(Long.valueOf(fileId)).orElseThrow();
        stored.setFileName("we\"ird.pdf");
        files.save(stored);

        mvc.perform(get("/api/files/" + fileId + "/content").header("Authorization", teacher))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"we\\\"ird.pdf\""));
    }

    // --- the old open routes are gone (P6, P7) ---

    @Test
    void oldFileRoutes_signedOut_areNotServed() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String course = createCourse(teacher, "Old-" + UUID.randomUUID());
        String fileId = uploadPdf(teacher, idOf(course));

        mvc.perform(get("/api/course-files/view/" + fileId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/course-files/course/" + idOf(course))).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/course-files/upload/" + accessCodeOf(course)).file(pdf()))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", UUID.randomUUID() + "@example.com",
                encoder.encode(PASSWORD), "default-profile.png"));
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

    private org.springframework.test.web.servlet.ResultActions join(String auth, String code) throws Exception {
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

    private static String idOf(String courseJson) {
        return JsonPath.read(courseJson, "$.id").toString();
    }

    private static String accessCodeOf(String courseJson) {
        return JsonPath.read(courseJson, "$.accessCode");
    }
}
