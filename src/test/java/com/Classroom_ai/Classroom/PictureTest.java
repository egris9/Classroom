package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S3 Course materials, picture side: profile pictures are stored through the material module and served
 * from {@code GET /api/users/{id}/picture} (P16 P28 P29 P30 P32).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PictureTest {

    private static final String PASSWORD = "secret-pw";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;

    @Test
    void signupWithPicture_isServedToAnyoneWithItsOwnContentType() throws Exception {
        String body = signup(picture("me.png", PNG)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id").toString();
        String url = JsonPath.read(body, "$.picture");

        MvcResult served = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();

        assertEquals("/api/users/" + id + "/picture", url);
        assertArrayEquals(PNG, served.getResponse().getContentAsByteArray());
    }

    @Test
    void signupWithoutPicture_hasNoPictureAndTheEndpointIs404() throws Exception {
        String body = signup(null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.picture").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/users/" + JsonPath.read(body, "$.id") + "/picture")).andExpect(status().isNotFound());
    }

    @Test
    void signupWithANonImage_is415AndTheEmailStaysFree() throws Exception {
        String email = UUID.randomUUID() + "@example.com";

        signupAs(email, picture("me.png", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        signupAs(email, null).andExpect(status().isCreated());
    }

    @Test
    void signupWithAPictureOver2Mb_is413() throws Exception {
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        signup(picture("me.png", big)).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void unknownUser_is404() throws Exception {
        mvc.perform(get("/api/users/999999/picture")).andExpect(status().isNotFound());
    }

    @Test
    void aStoredKeyOutsideThePicturesFolder_isNeverServed() throws Exception {
        for (String key : new String[]{"default-profile.png", "../application.properties", "files/x.pdf", "C:\\Windows\\win.ini", "/etc/passwd"}) {
            User user = users.save(new User("Bad", "Key", UUID.randomUUID() + "@example.com", encoder.encode(PASSWORD), key));

            mvc.perform(get("/api/users/" + user.getId() + "/picture")).andExpect(status().isNotFound());
        }
    }

    @Test
    void courseTeacher_carriesThePictureUrlOrNull() throws Exception {
        String signedUp = signup(picture("me.png", PNG)).andReturn().getResponse().getContentAsString();
        String withPicture = bearer(users.findById(Long.valueOf(JsonPath.read(signedUp, "$.id").toString())).orElseThrow());
        String without = bearer(users.save(new User("No", "Pic", UUID.randomUUID() + "@example.com", encoder.encode(PASSWORD), null)));

        mvc.perform(post("/api/courses").header("Authorization", withPicture).contentType(MediaType.APPLICATION_JSON).content(courseJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacher.picture").value(JsonPath.read(signedUp, "$.picture").toString()));
        mvc.perform(post("/api/courses").header("Authorization", without).contentType(MediaType.APPLICATION_JSON).content(courseJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacher.picture").value(nullValue()));
    }

    // --- helpers ---

    private ResultActions signup(MockMultipartFile picture) throws Exception {
        return signupAs(UUID.randomUUID() + "@example.com", picture);
    }

    private ResultActions signupAs(String email, MockMultipartFile picture) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/auth/signup");
        if (picture != null) {
            request.file(picture);
        }
        request.param("firstName", "Ada").param("lastName", "Lovelace")
                .param("email", email).param("password", PASSWORD);
        return mvc.perform(request);
    }

    private static MockMultipartFile picture(String name, byte[] bytes) {
        return new MockMultipartFile("profilePicture", name, "image/png", bytes);
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    private static String courseJson() {
        return "{\"courseName\":\"P-" + UUID.randomUUID() + "\",\"section\":\"A\",\"subject\":\"Maths\",\"room\":1}";
    }
}
