package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.tools.TrialGate;
import com.Classroom_ai.Classroom.tools.TrialUse;
import com.Classroom_ai.Classroom.tools.TrialUseRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One free use of the AI tools per visitor. The gate is driven over HTTP through a probe route that calls it the way
 * the real tools routes do (they come in Task 3.3 and 3.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ToolsTrialTest.Probe.class)
class ToolsTrialTest extends ToolsTestSupport {

    /** Test-only: a route under the open {@code /api/auth/**} that does nothing but ask the gate. */
    @TestConfiguration
    static class Probe {
        @Bean
        ProbeController probeController(TrialGate gate, UserService users) {
            return new ProbeController(gate, users);
        }
    }

    @RestController
    static class ProbeController {
        private final TrialGate gate;
        private final UserService users;

        ProbeController(TrialGate gate, UserService users) {
            this.gate = gate;
            this.users = users;
        }

        @PostMapping("/api/auth/trial-probe")
        ResponseEntity<Void> probe(HttpServletRequest request, HttpServletResponse response) {
            gate.admit(request, response, users.findAuthenticatedUser().orElse(null));
            return ResponseEntity.noContent().build();
        }
    }

    @Autowired TrialUseRepository trialUses;

    private ResultActions probe(String ip, String cookie, String bearer, String forwardedFor) throws Exception {
        var request = post("/api/auth/trial-probe").with(from(ip));
        if (cookie != null) {
            request.cookie(new Cookie("trial", cookie));
        }
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return mvc.perform(request);
    }

    private ResultActions probe(String ip) throws Exception {
        return probe(ip, null, null, null);
    }

    private String cookieSetBy(ResultActions result) {
        return trialCookieIn(result.andReturn().getResponse().getHeader("Set-Cookie"));
    }

    private ResultActions trialStatus(String ip, String cookie, String bearer) throws Exception {
        var request = get("/api/tools/trial").with(from(ip));
        if (cookie != null) {
            request.cookie(new Cookie("trial", cookie));
        }
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        return mvc.perform(request);
    }

    @Test
    void firstAnonymousCallIsAdmittedAndGetsAYearLongHttpOnlyCookie() throws Exception {
        String header = probe(newIp()).andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(header).matches("trial=[0-9a-f-]{36}; .*")
                .contains("Max-Age=31536000").contains("Path=/").contains("HttpOnly").contains("SameSite=Lax");
    }

    @Test
    void secondCallWithSameCookieIsRefused() throws Exception {
        String cookie = cookieSetBy(probe(newIp()).andExpect(status().isNoContent()));

        probe(newIp(), cookie, null, null).andExpect(status().isForbidden());
    }

    @Test
    void secondCallWithNoCookieFromSameIpIsRefused() throws Exception {
        String ip = newIp();
        probe(ip).andExpect(status().isNoContent());

        probe(ip).andExpect(status().isForbidden());
    }

    @Test
    void forgedForwardedForDoesNotGiveAnotherUse() throws Exception {
        String ip = newIp();
        probe(ip).andExpect(status().isNoContent());

        for (String forged : new String[]{"203.0.113.50", "203.0.113.51, 203.0.113.52", "10.99.99.99"}) {
            probe(ip, null, null, forged).andExpect(status().isForbidden());
        }
    }

    @Test
    void differentIpAndNoCookieIsAdmitted() throws Exception {
        probe(newIp()).andExpect(status().isNoContent());

        probe(newIp()).andExpect(status().isNoContent());
    }

    @Test
    void signedInUserIsNeverRefusedAndDoesNotUseUpTheVisitorsTrial() throws Exception {
        String ip = newIp();
        String auth = bearer(newUser("Signed"));

        for (int i = 0; i < 3; i++) {
            var result = probe(ip, null, auth, null).andExpect(status().isNoContent());
            assertThat(result.andReturn().getResponse().getHeader("Set-Cookie")).isNull();
        }

        probe(ip).andExpect(status().isNoContent());
    }

    @Test
    void aTokenForAnAccountThatNoLongerExistsIsOnlyAnAnonymousVisitor() throws Exception {
        String ip = newIp();
        User gone = new User("Gone", "Test", UUID.randomUUID() + "@example.com", "unused", "default-profile.png");
        String auth = bearer(gone);
        probe(ip, null, auth, null).andExpect(status().isNoContent());

        probe(ip, null, auth, null).andExpect(status().isForbidden());
    }

    @Test
    void refusalIsA403WithCodeTrialUsed() throws Exception {
        String ip = newIp();
        probe(ip).andExpect(status().isNoContent());

        probe(ip).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRIAL_USED"))
                .andExpect(jsonPath("$.message").value("Sign up to keep using the AI tools."));
    }

    @Test
    void parallelCallsFromOneVisitorShareOneUse() throws Exception {
        String ip = newIp();
        int callers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (int i = 0; i < callers; i++) {
            Callable<Integer> call = () -> {
                go.await();
                return probe(ip).andReturn().getResponse().getStatus();
            };
            statuses.add(pool.submit(call));
        }
        go.countDown();

        List<Integer> seen = new ArrayList<>();
        for (Future<Integer> status : statuses) {
            seen.add(status.get());
        }
        pool.shutdown();

        assertThat(seen).filteredOn(code -> code == 204).hasSize(1);
        assertThat(seen).filteredOn(code -> code == 403).hasSize(callers - 1);
    }

    @Test
    void trialStatusDoesNotConsume() throws Exception {
        String ip = newIp();

        for (int i = 0; i < 2; i++) {
            trialStatus(ip, null, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.signedIn").value(false))
                    .andExpect(jsonPath("$.trialAvailable").value(true));
        }
        String cookie = cookieSetBy(probe(ip).andExpect(status().isNoContent()));

        trialStatus(ip, null, null).andExpect(jsonPath("$.trialAvailable").value(false));
        trialStatus(newIp(), cookie, null).andExpect(jsonPath("$.trialAvailable").value(false));
        trialStatus(newIp(), null, null).andExpect(jsonPath("$.trialAvailable").value(true));
    }

    @Test
    void trialStatusOfASignedInUserNeedsNoTrial() throws Exception {
        trialStatus(newIp(), null, bearer(newUser("Signed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedIn").value(true))
                .andExpect(jsonPath("$.trialAvailable").value(false));
    }

    @Test
    void storedTrialsHoldHashesNotTheCookieOrTheAddress() throws Exception {
        String ip = newIp();
        String cookie = cookieSetBy(probe(ip).andExpect(status().isNoContent()));

        List<TrialUse> rows = trialUses.findAll();

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getCookieHash()).matches("[0-9a-f]{64}").isNotEqualTo(cookie);
            assertThat(row.getIpHash()).matches("[0-9a-f]{64}").isNotEqualTo(ip);
        });
    }

    @Test
    void aBlankSaltRefusesToStart() {
        for (String blank : new String[]{"", "  ", null}) {
            assertThatThrownBy(() -> new TrialGate(null, blank, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("TOOLS_TRIAL_SALT");
        }
    }
}
