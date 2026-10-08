package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.course.Course;
import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileRepository;
import com.Classroom_ai.Classroom.course.CourseRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S3 Course materials, course-file side: how an upload is named, checked and stored (P15 P21 P28 P29 P30 P31).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MaterialTest {

    private static final String PASSWORD = "secret-pw";
    private static final int LIMIT = 20 * 1024 * 1024;

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;
    @Autowired CourseFileRepository files;
    @Autowired CourseRepository courses;
    @Value("${upload.dir}") String uploadDir;

    @TempDir Path elsewhere;

    // --- P15, P28: the stored name is ours, the client's name is only a label ---

    @Test
    void upload_withTraversalName_isStoredUnderFilesWithAUuidName() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = courseId(teacher);

        String body = mvc.perform(multipart("/api/courses/" + courseId + "/files")
                        .file(new MockMultipartFile("file", "../../x.pdf", "application/pdf", pdfBytes(100)))
                        .header("Authorization", teacher))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("x.pdf"))
                .andReturn().getResponse().getContentAsString();

        CourseFile stored = files.findById(Long.valueOf(JsonPath.read(body, "$.id").toString())).orElseThrow();
        assertTrue(stored.getFilePath().matches("files/[0-9a-f-]{36}\\.pdf"), stored.getFilePath());
        assertTrue(Files.isRegularFile(Path.of(uploadDir).resolve(stored.getFilePath())));
        assertFalse(Files.exists(Path.of(uploadDir).resolve("files").resolve("../../x.pdf").normalize()));
    }

    @Test
    void upload_withBackslashName_keepsOnlyTheLastSegmentAsDisplayName() throws Exception {
        String teacher = bearer(newUser("Teach"));

        mvc.perform(multipart("/api/courses/" + courseId(teacher) + "/files")
                        .file(new MockMultipartFile("file", "C:\\Users\\me\\notes.pdf", "application/pdf", pdfBytes(100)))
                        .header("Authorization", teacher))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("notes.pdf"));
    }

    // --- P30: type check by magic bytes ---

    @Test
    void upload_nonPdfRenamedToPdf_is415AndStoresNothing() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = courseId(teacher);
        long before = storedCount();

        mvc.perform(multipart("/api/courses/" + courseId + "/files")
                        .file(new MockMultipartFile("file", "notes.pdf", "application/pdf",
                                "<html>not a pdf</html>".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", teacher))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        assertEquals(before, storedCount());
    }

    @Test
    void upload_emptyFile_is415() throws Exception {
        String teacher = bearer(newUser("Teach"));

        mvc.perform(multipart("/api/courses/" + courseId(teacher) + "/files")
                        .file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                        .header("Authorization", teacher))
                .andExpect(status().isUnsupportedMediaType());
    }

    // --- P21: size limit ---

    @Test
    void upload_2MbPdf_succeeds() throws Exception {
        String teacher = bearer(newUser("Teach"));

        mvc.perform(multipart("/api/courses/" + courseId(teacher) + "/files")
                        .file(new MockMultipartFile("file", "big.pdf", "application/pdf", pdfBytes(2 * 1024 * 1024)))
                        .header("Authorization", teacher))
                .andExpect(status().isCreated());
    }

    @Test
    void upload_atTheLimit_succeeds() throws Exception {
        String teacher = bearer(newUser("Teach"));

        mvc.perform(multipart("/api/courses/" + courseId(teacher) + "/files")
                        .file(new MockMultipartFile("file", "max.pdf", "application/pdf", pdfBytes(LIMIT)))
                        .header("Authorization", teacher))
                .andExpect(status().isCreated());
    }

    @Test
    void upload_overTheLimit_is413AndStoresNothing() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = courseId(teacher);
        long before = storedCount();

        mvc.perform(multipart("/api/courses/" + courseId + "/files")
                        .file(new MockMultipartFile("file", "huge.pdf", "application/pdf", pdfBytes(LIMIT + 1)))
                        .header("Authorization", teacher))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

        assertEquals(before, storedCount());
    }

    // --- P31: a row holding an absolute path (old data) is never followed ---

    @Test
    void content_ofARowWithAnAbsolutePath_is404() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = courseId(teacher);
        Path outside = Files.writeString(elsewhere.resolve("secret.pdf"), "%PDF-1.4 secret");
        CourseFile legacy = new CourseFile();
        legacy.setFileName("secret.pdf");
        legacy.setFilePath(outside.toString());
        legacy.setCourse(courseOf(courseId));
        legacy = files.save(legacy);

        mvc.perform(get("/api/files/" + legacy.getId() + "/content").header("Authorization", teacher))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void content_ofARowWithADotDotKey_is404() throws Exception {
        String teacher = bearer(newUser("Teach"));
        String courseId = courseId(teacher);
        Path sibling = Files.writeString(Path.of(uploadDir).resolve("sibling.pdf"), "%PDF-1.4 sibling");
        CourseFile bad = new CourseFile();
        bad.setFileName("sibling.pdf");
        bad.setFilePath("files/../" + sibling.getFileName());
        bad.setCourse(courseOf(courseId));
        bad = files.save(bad);

        mvc.perform(get("/api/files/" + bad.getId() + "/content").header("Authorization", teacher))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private long storedCount() throws Exception {
        Path dir = Path.of(uploadDir).resolve("files");
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        }
    }

    private static byte[] pdfBytes(int size) {
        byte[] bytes = new byte[size];
        byte[] magic = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, bytes, 0, Math.min(magic.length, size));
        return bytes;
    }

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", UUID.randomUUID() + "@example.com",
                encoder.encode(PASSWORD), null));
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    private String courseId(String auth) throws Exception {
        String body = mvc.perform(post("/api/courses").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"M-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id").toString();
    }

    private Course courseOf(String id) {
        return courses.findById(Long.valueOf(id)).orElseThrow();
    }
}
