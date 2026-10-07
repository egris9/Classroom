package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.User.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S1: sign-in, sign-up and token handling (P11, P13, P14, P17, P18). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthTest {

    private static final String PASSWORD = "secret-pw";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Value("${jwt.secret}") String jwtSecret;

    @Test
    void signin_withJsonBody_returnsTokenAndUser() throws Exception {
        User user = newUser();
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.user.id").value(user.getId()))
                .andExpect(jsonPath("$.user.firstName").value("Grace"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void signin_withWrongPassword_returns401() throws Exception {
        User user = newUser();
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(user.getEmail(), "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    @Test
    void signin_withUnknownEmail_returns401() throws Exception {
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void signup_returnsCreatedUserWithoutPasswordHash() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/signup")
                        .param("firstName", "Ada").param("lastName", "Lovelace")
                        .param("email", email).param("password", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.picture").value("default-profile.png"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/courses/courses").param("type", "any"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedRoute_withGarbageToken_returns401() throws Exception {
        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedRoute_withExpiredToken_returns401() throws Exception {
        String token = tokenSignedWith(jwtSecret, new Date(System.currentTimeMillis() - 1000));
        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedRoute_withTokenSignedByAnotherSecret_returns401() throws Exception {
        User user = newUser();
        String otherSecret = "some-other-secret-some-other-secret-some-other-secret-9876543210";
        String token = Jwts.builder().setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes()), SignatureAlgorithm.HS256).compact();
        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidToken_isAllowed() throws Exception {
        User user = newUser();
        String token = tokenSignedWith(jwtSecret, new Date(System.currentTimeMillis() + 60_000), user.getEmail());
        mvc.perform(get("/api/courses/courses").param("type", "any")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String tokenSignedWith(String secret, Date expiry) {
        return tokenSignedWith(secret, expiry, uniqueEmail());
    }

    private String tokenSignedWith(String secret, Date expiry, String email) {
        return Jwts.builder().setSubject(email).setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256).compact();
    }

    private User newUser() {
        return users.save(new User("Grace", "Hopper", uniqueEmail(), encoder.encode(PASSWORD), "default-profile.png"));
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
