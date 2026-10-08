package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MockMvc skips the servlet container, so it cannot see the multipart limit. This goes over a real socket (P21:
 * Spring's 1MB default used to reject any PDF above 1MB).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadLimitHttpTest {

    @Autowired TestRestTemplate http;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;

    @Test
    void upload_2MbPdf_overARealConnection_succeeds() {
        HttpHeaders auth = auth();
        String courseId = createCourse(auth);

        ResponseEntity<String> response = upload(courseId, auth, pdf(2 * 1024 * 1024));

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
    }

    @Test
    void upload_3MbPdf_overARealConnection_succeeds() {
        HttpHeaders auth = auth();

        ResponseEntity<String> response = upload(createCourse(auth), auth, pdf(3 * 1024 * 1024));

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
    }

    private ResponseEntity<String> upload(String courseId, HttpHeaders auth, byte[] bytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "big.pdf";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(auth);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.postForEntity("/api/courses/" + courseId + "/files", new HttpEntity<>(body, headers), String.class);
    }

    private String createCourse(HttpHeaders auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"courseName\":\"H-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}";
        ResponseEntity<Map> created = http.postForEntity("/api/courses", new HttpEntity<>(json, headers), Map.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        return created.getBody().get("id").toString();
    }

    private HttpHeaders auth() {
        User user = users.save(new User("Teach", "Test", UUID.randomUUID() + "@example.com", encoder.encode("secret-pw"), null));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.generateToken(user));
        return headers;
    }

    private static byte[] pdf(int size) {
        byte[] bytes = new byte[size];
        byte[] magic = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }
}
