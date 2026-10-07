package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the behaviour of the user endpoints as they were before the refactor.
 * These tests describe what the code does today, including its flaws
 * (see docs/PROBLEMS_AND_CONTEXT.md), so they pass as written.
 * The course and file endpoints that used to be pinned here were replaced in S2:
 * see {@link MembershipTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndpointBaselineTest {

    private static final String PASSWORD = "secret-pw";

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

    // --- helpers ---

    private User newUser(String firstName) {
        return users.save(new User(firstName, "Test", uniqueEmail(), encoder.encode(PASSWORD), "default-profile.png"));
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
