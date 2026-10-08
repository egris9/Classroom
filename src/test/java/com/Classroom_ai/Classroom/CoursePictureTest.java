package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.course.CourseRepository;
import com.Classroom_ai.Classroom.course.CourseService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Round 2, Part 2: an optional cover picture on a Course, Teacher only to change, open to read. */
@SpringBootTest
@AutoConfigureMockMvc
class CoursePictureTest {

    private static final String PASSWORD = "secret-pw";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Autowired CourseService courseService;
    @Autowired PlatformTransactionManager txManager;
    @Value("${upload.dir}") String uploadDir;

    @Test
    void teacherCanSetCoverAndResponseCarriesItsPath() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);

        put(id, teacher, cover(PNG)).andExpect(status().isOk())
                .andExpect(jsonPath("$.picture").value("/api/courses/" + id + "/picture"))
                .andExpect(jsonPath("$.id").value(id));

        mvc.perform(get("/api/courses/" + id).header("Authorization", teacher))
                .andExpect(jsonPath("$.picture").value("/api/courses/" + id + "/picture"));
    }

    @Test
    void courseWithoutCoverHasNullPicture() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);

        mvc.perform(get("/api/courses/" + id).header("Authorization", teacher))
                .andExpect(jsonPath("$.picture").value(nullValue()));
    }

    @Test
    void studentSettingCoverGets403() throws Exception {
        String teacher = bearer(newUser());
        String student = bearer(newUser());
        long id = joinedCourse(teacher, student);

        put(id, student, cover(PNG)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/courses/" + id + "/picture").header("Authorization", student))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberGets403() throws Exception {
        long id = createCourse(bearer(newUser()));

        put(id, bearer(newUser()), cover(PNG)).andExpect(status().isForbidden());
    }

    @Test
    void signedOutGets401() throws Exception {
        long id = createCourse(bearer(newUser()));

        mvc.perform(multipart(HttpMethod.PUT, "/api/courses/" + id + "/picture").file(cover(PNG)))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/courses/" + id + "/picture")).andExpect(status().isUnauthorized());
    }

    @Test
    void getCoverIsOpenAndReturnsStoredType() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);
        put(id, teacher, cover(JPEG)).andExpect(status().isOk());

        byte[] served = mvc.perform(get("/api/courses/" + id + "/picture"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(JPEG, served);
    }

    @Test
    void getCoverWithNoneIs404() throws Exception {
        long id = createCourse(bearer(newUser()));

        mvc.perform(get("/api/courses/" + id + "/picture")).andExpect(status().isNotFound());
    }

    @Test
    void renamedTextFileGets415() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);

        put(id, teacher, cover("<html>not an image</html>".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void overTwoMegabytesGets413() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        put(id, teacher, cover(big)).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void replacingCoverDeletesTheOldFile() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);
        put(id, teacher, cover(PNG)).andExpect(status().isOk());
        Path oldFile = coverFile(id);
        assertTrue(Files.exists(oldFile));

        put(id, teacher, cover(JPEG)).andExpect(status().isOk());

        assertFalse(Files.exists(oldFile), "old cover file should be gone");
        assertTrue(Files.exists(coverFile(id)));
    }

    @Test
    void removingCoverDeletesTheFileAndNullsPicture() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);
        put(id, teacher, cover(PNG)).andExpect(status().isOk());
        Path file = coverFile(id);

        mvc.perform(delete("/api/courses/" + id + "/picture").header("Authorization", teacher))
                .andExpect(status().isNoContent());

        assertFalse(Files.exists(file));
        assertNull(courses.findById(id).orElseThrow().getPicturePath());
        mvc.perform(get("/api/courses/" + id).header("Authorization", teacher))
                .andExpect(jsonPath("$.picture").value(nullValue()));
    }

    @Test
    void missingCourseGets404() throws Exception {
        String teacher = bearer(newUser());

        put(999999L, teacher, cover(PNG)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/courses/999999/picture").header("Authorization", teacher))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingTheCourseDeletesItsCoverFile() throws Exception {
        String teacher = bearer(newUser());
        long id = createCourse(teacher);
        put(id, teacher, cover(PNG)).andExpect(status().isOk());
        Path file = coverFile(id);
        assertTrue(Files.exists(file));

        mvc.perform(delete("/api/courses/" + id).header("Authorization", teacher))
                .andExpect(status().isNoContent());

        assertFalse(Files.exists(file), "cover file should be deleted with the course");
    }

    @Test
    void rolledBackCoverChangeLeavesNoOrphanFileAndKeepsTheOldCover() throws Exception {
        User owner = newUser();
        String teacher = bearer(owner);
        long id = createCourse(teacher);
        put(id, teacher, cover(PNG)).andExpect(status().isOk());
        Path oldFile = coverFile(id);
        Path pictures = oldFile.getParent();
        long before = Files.list(pictures).count();

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            try {
                courseService.setPicture(owner, id, cover(JPEG));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            status.setRollbackOnly();
        });

        assertEquals(before, Files.list(pictures).count(), "the new file should be removed on rollback");
        assertTrue(Files.exists(oldFile), "the old cover must survive a rollback");
    }

    // --- helpers ---

    private Path coverFile(long courseId) {
        String key = courses.findById(courseId).orElseThrow().getPicturePath();
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(key);
    }

    private ResultActions put(long id, String token, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/courses/" + id + "/picture").file(file).header("Authorization", token));
    }

    private static MockMultipartFile cover(byte[] bytes) {
        return new MockMultipartFile("picture", "cover.png", "image/png", bytes);
    }

    private long createCourse(String token) throws Exception {
        String body = mvc.perform(post("/api/courses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"C-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long joinedCourse(String teacher, String student) throws Exception {
        String body = mvc.perform(post("/api/courses").header("Authorization", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"J-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/courses/join").header("Authorization", student).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"" + JsonPath.read(body, "$.accessCode") + "\"}"))
                .andExpect(status().isOk());
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private User newUser() {
        return users.save(new User("Test", "User", UUID.randomUUID() + "@example.com", encoder.encode(PASSWORD), null));
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }
}
