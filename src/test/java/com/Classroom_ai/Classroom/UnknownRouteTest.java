package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserRepository;
import com.Classroom_ai.Classroom.auth.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A request the server has no route for is the caller's mistake (404, 405), not a server error. */
@SpringBootTest
@AutoConfigureMockMvc
class UnknownRouteTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtTokenUtil jwt;

    @Test
    void unknownPath_is404NotFound() throws Exception {
        mvc.perform(get("/api/no-such-thing").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void knownPathWithAnUnsupportedMethod_is405() throws Exception {
        mvc.perform(put("/api/courses").header("Authorization", bearer()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void deleteOnAPathThatOnlyReads_is405() throws Exception {
        mvc.perform(delete("/api/courses").header("Authorization", bearer()))
                .andExpect(status().isMethodNotAllowed());
    }

    private String bearer() {
        User user = users.save(new User("Route", "Test", UUID.randomUUID() + "@example.com", encoder.encode("secret-pw"), null));
        return "Bearer " + jwt.generateToken(user);
    }
}
