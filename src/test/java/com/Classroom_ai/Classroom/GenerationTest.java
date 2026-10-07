package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.generation.Exercise;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S4 Generation, fake model. Summaries and Exercise sets are requested on a CourseFile, run in the background
 * through the {@link TextGeneration} seam, and polled with GET. A Teacher's result is published to the course;
 * a Student's is private to that Student.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GenerationTest {

    private static final String PASSWORD = "secret-pw";
    private static final String LESSON = "Photosynthesis turns light into chemical energy in the chloroplasts of plant cells.";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Autowired TextGeneration textGeneration;

    // --- the fake adapter ---

    @Test
    void fakeAdapter_isDeterministic_andNamesItself() {
        assertEquals(textGeneration.summarize(LESSON), textGeneration.summarize(LESSON));
        List<Exercise> exercises = textGeneration.generateExercises(LESSON, 5);
        assertEquals(5, exercises.size());
        assertEquals(exercises, textGeneration.generateExercises(LESSON, 5));
        assertEquals("fake", textGeneration.modelName());
    }

    // --- POST /api/files/{id}/summaries (member) ---

    @Test
    void summary_byTeacher_is202ThenDoneAndPublished() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("summaries", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        String list = awaitDone("summaries", file, c.teacher);
        assertEquals("DONE", JsonPath.read(list, "$[0].status"));
        assertEquals(true, JsonPath.read(list, "$[0].published"));
        assertEquals("fake", JsonPath.read(list, "$[0].model"));
        assertFalse(((String) JsonPath.read(list, "$[0].text")).isBlank());
    }

    @Test
    void summary_byTeacher_isVisibleToAStudentOfTheCourse() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());
        awaitDone("summaries", file, c.teacher);

        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", c.student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].published").value(true));
    }

    @Test
    void summary_byStudent_isPrivateToThatStudent() throws Exception {
        Course c = course();
        String otherStudent = c.enrol("Other");
        String file = c.uploadText(LESSON);

        c.generate("summaries", file, c.student).andExpect(status().isAccepted());
        String own = awaitDone("summaries", file, c.student);
        assertEquals(false, JsonPath.read(own, "$[0].published"));

        for (String other : List.of(c.teacher, otherStudent)) {
            mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", other))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void summary_nonMember_is403() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        String outsider = bearer(newUser("Outsider"));

        c.generate("summaries", file, outsider).andExpect(status().isForbidden());
        mvc.perform(get("/api/files/" + file + "/summaries").header("Authorization", outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_signedOut_is401() throws Exception {
        mvc.perform(post("/api/files/1/summaries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/files/1/summaries")).andExpect(status().isUnauthorized());
    }

    @Test
    void summary_ofMissingFile_is404() throws Exception {
        Course c = course();
        c.generate("summaries", "999999", c.teacher).andExpect(status().isNotFound());
        mvc.perform(get("/api/files/999999/summaries").header("Authorization", c.teacher))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/files/{id}/exercise-sets (teacher) ---

    @Test
    void exerciseSet_byTeacher_hasFiveQuestionsWithModelAnswers() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("exercise-sets", file, c.teacher)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        String list = awaitDone("exercise-sets", file, c.teacher);
        assertEquals(true, JsonPath.read(list, "$[0].published"));
        assertEquals("fake", JsonPath.read(list, "$[0].model"));
        assertEquals(5, ((List<?>) JsonPath.read(list, "$[0].exercises")).size());
        assertFalse(((String) JsonPath.read(list, "$[0].exercises[0].question")).isBlank());
        assertFalse(((String) JsonPath.read(list, "$[0].exercises[0].answer")).isBlank());
    }

    @Test
    void exerciseSet_byTeacher_isVisibleToAStudent() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());
        awaitDone("exercise-sets", file, c.teacher);

        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", c.student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void exerciseSet_byStudent_is403() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);

        c.generate("exercise-sets", file, c.student).andExpect(status().isForbidden());
    }

    @Test
    void exerciseSet_nonMemberAndSignedOut_are403And401() throws Exception {
        Course c = course();
        String file = c.uploadText(LESSON);
        String outsider = bearer(newUser("Outsider"));

        c.generate("exercise-sets", file, outsider).andExpect(status().isForbidden());
        mvc.perform(get("/api/files/" + file + "/exercise-sets").header("Authorization", outsider))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/files/" + file + "/exercise-sets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/files/" + file + "/exercise-sets")).andExpect(status().isUnauthorized());
    }

    @Test
    void exerciseSet_ofMissingFile_is404() throws Exception {
        Course c = course();
        c.generate("exercise-sets", "999999", c.teacher).andExpect(status().isNotFound());
    }

    // --- failures the user must see ---

    @Test
    void scannedPdfWithNoText_failsWithNoText() throws Exception {
        Course c = course();
        String file = c.upload(pdfBytes(null));

        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("summaries", file, c.teacher, "FAILED");
        assertEquals("NO_TEXT", JsonPath.read(list, "$[0].failureCode"));
        assertFalse(((String) JsonPath.read(list, "$[0].failureMessage")).isBlank());
    }

    @Test
    void exerciseSetOnScannedPdf_failsWithNoText() throws Exception {
        Course c = course();
        String file = c.upload(pdfBytes(null));

        c.generate("exercise-sets", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("exercise-sets", file, c.teacher, "FAILED");
        assertEquals("NO_TEXT", JsonPath.read(list, "$[0].failureCode"));
    }

    @Test
    void corruptPdf_failsWithPdfUnreadable() throws Exception {
        Course c = course();
        String file = c.upload("%PDF-1.4 this is not a real document".getBytes(StandardCharsets.UTF_8));

        c.generate("summaries", file, c.teacher).andExpect(status().isAccepted());

        String list = awaitStatus("summaries", file, c.teacher, "FAILED");
        assertEquals("PDF_UNREADABLE", JsonPath.read(list, "$[0].failureCode"));
    }

    // --- helpers ---

    private String awaitDone(String kind, String file, String auth) {
        return awaitStatus(kind, file, auth, "DONE");
    }

    private String awaitStatus(String kind, String file, String auth, String expected) {
        String[] body = new String[1];
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
            body[0] = mvc.perform(get("/api/files/" + file + "/" + kind).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertEquals(expected, JsonPath.read(body[0], "$[0].status"));
        });
        return body[0];
    }

    private Course course() throws Exception {
        return new Course();
    }

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", UUID.randomUUID() + "@example.com",
                encoder.encode(PASSWORD), "default-profile.png"));
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    /** {@code text} drawn on one page, or a blank page when null (what a scan without OCR looks like). */
    private static byte[] pdfBytes(String text) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null) {
                try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** A course with a Teacher and one enrolled Student. */
    private class Course {
        final String teacher = bearer(newUser("Teach"));
        final String student;
        final String id;

        Course() throws Exception {
            String body = mvc.perform(post("/api/courses").header("Authorization", teacher)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"courseName\":\"Gen-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Bio\",\"room\":1}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            id = JsonPath.read(body, "$.id").toString();
            accessCode = JsonPath.read(body, "$.accessCode");
            student = enrol("Stud");
        }

        private final String accessCode;

        String enrol(String name) throws Exception {
            String auth = bearer(newUser(name));
            mvc.perform(post("/api/courses/join").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"accessCode\":\"" + accessCode + "\"}"))
                    .andExpect(status().isOk());
            return auth;
        }

        String uploadText(String text) throws Exception {
            return upload(pdfBytes(text));
        }

        String upload(byte[] bytes) throws Exception {
            String body = mvc.perform(multipart("/api/courses/" + id + "/files")
                            .file(new MockMultipartFile("file", "lesson.pdf", "application/pdf", bytes))
                            .header("Authorization", teacher))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.read(body, "$.id").toString();
        }

        ResultActions generate(String kind, String file, String auth) throws Exception {
            return mvc.perform(post("/api/files/" + file + "/" + kind).header("Authorization", auth));
        }
    }
}
