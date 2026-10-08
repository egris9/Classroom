package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A course with a Teacher and a Student, uploaded PDFs, and polling, for the tests that drive generation over HTTP. */
abstract class GenerationTestSupport {

    static final String PASSWORD = "secret-pw";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;

    String awaitDone(String kind, String file, String auth) {
        return awaitStatus(kind, file, auth, "DONE");
    }

    String awaitStatus(String kind, String file, String auth, String expected) {
        String[] body = new String[1];
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
            body[0] = mvc.perform(get("/api/files/" + file + "/" + kind).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertEquals(expected, JsonPath.read(body[0], "$[0].status"));
        });
        return body[0];
    }

    Course course() throws Exception {
        return new Course();
    }

    User newUser(String firstName) {
        return users.save(new User(firstName, "Test", UUID.randomUUID() + "@example.com",
                encoder.encode(PASSWORD), "default-profile.png"));
    }

    String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    /** {@code text} drawn on one page, or a blank page when null (what a scan without OCR looks like). */
    static byte[] pdfBytes(String text) throws Exception {
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
    class Course {
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
