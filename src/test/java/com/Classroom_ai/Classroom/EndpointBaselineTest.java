package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the behaviour of the nine endpoints as they were before the refactor.
 * These tests describe what the code does today, including its flaws
 * (see docs/PROBLEMS_AND_CONTEXT.md), so they pass as written.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndpointBaselineTest {

    private static final String PASSWORD = "secret-pw";
    private static final String PDF = "%PDF-1.4 baseline";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Value("${upload.dir}") String uploadDir;

    @BeforeAll
    static void createUploadDir(@Value("${upload.dir}") String dir) throws Exception {
        Files.createDirectories(Paths.get(dir));
    }

    // --- UserController ---

    @Test
    void signup_createsUser() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/signup")
                        .param("firstName", "Ada").param("lastName", "Lovelace")
                        .param("email", email).param("password", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.picture").value("default-profile.png"));
    }

    @Test
    void signin_returnsTokenAndUser() throws Exception {
        User user = newUser("Grace");
        mvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.user.firstName").value("Grace"));
    }

    @Test
    void profilePicture_servesFileFromUploadDir() throws Exception {
        String name = UUID.randomUUID() + "-me.jpg";
        Files.write(Path.of(uploadDir).resolve(name), new byte[]{1, 2, 3, 4});

        MvcResult result = mvc.perform(get("/api/auth/profile-picture/" + name))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();
        assertEquals(4, result.getResponse().getContentAsByteArray().length);
    }

    // --- CourseController ---

    @Test
    void createCourse_returnsAccessCode() throws Exception {
        String token = tokenFor(newUser("Teach"));
        mvc.perform(post("/api/courses/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson("Math-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessCode").value(notNullValue()));
    }

    @Test
    void joinCourse_addsStudentAndReturnsCourse() throws Exception {
        String name = "Join-" + UUID.randomUUID();
        String code = createCourse(tokenFor(newUser("Teach")), name);
        String studentToken = tokenFor(newUser("Stud"));

        mvc.perform(get("/api/courses/join").param("accessCode", code)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseName").value(name))
                .andExpect(jsonPath("$.accessCode").value(code));
    }

    @Test
    void getCourses_listsCreatedAndJoined() throws Exception {
        String teacherToken = tokenFor(newUser("Teach"));
        String code = createCourse(teacherToken, "Own-" + UUID.randomUUID());
        String studentToken = tokenFor(newUser("Stud"));
        mvc.perform(get("/api/courses/join").param("accessCode", code)
                .header("Authorization", "Bearer " + studentToken)).andExpect(status().isOk());

        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("created"));
        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("joined"));
    }

    // --- CourseFileController ---

    @Test
    void uploadFile_storesPdfForCourseByAccessCode() throws Exception {
        String code = createCourse(tokenFor(newUser("Teach")), "Up-" + UUID.randomUUID());

        mvc.perform(multipart("/api/course-files/upload/" + code).file(pdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("notes.pdf"))
                .andExpect(jsonPath("$.id").value(notNullValue()));
    }

    @Test
    void getFilesByCourse_listsUploadedFiles() throws Exception {
        String token = tokenFor(newUser("Teach"));
        String code = createCourse(token, "List-" + UUID.randomUUID());
        mvc.perform(multipart("/api/course-files/upload/" + code).file(pdf())).andExpect(status().isOk());

        mvc.perform(get("/api/course-files/course/" + onlyCourseIdOf(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName").value("notes.pdf"));
    }

    @Test
    void viewPdf_streamsStoredFile() throws Exception {
        String code = createCourse(tokenFor(newUser("Teach")), "View-" + UUID.randomUUID());
        MvcResult uploaded = mvc.perform(multipart("/api/course-files/upload/" + code).file(pdf()))
                .andExpect(status().isOk()).andReturn();
        String id = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id").toString();

        MvcResult result = mvc.perform(get("/api/course-files/view/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();
        assertEquals(PDF, result.getResponse().getContentAsString());
    }

    // --- helpers ---

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", uniqueEmail(), encoder.encode(PASSWORD), "default-profile.png"));
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private String tokenFor(User user) {
        return jwt.generateToken(user);
    }

    private static String courseJson(String name) {
        return "{\"courseName\":\"" + name + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}";
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "notes.pdf", "application/pdf", PDF.getBytes(StandardCharsets.UTF_8));
    }

    private String createCourse(String token, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/courses/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseJson(name)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessCode");
    }

    /** The id of the only course the token's user teaches. */
    private String onlyCourseIdOf(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/courses/courses").param("type", "any")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$[0].id").toString();
    }
}
