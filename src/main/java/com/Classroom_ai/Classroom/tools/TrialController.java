package com.Classroom_ai.Classroom.tools;

import com.Classroom_ai.Classroom.api.TrialStatusResponse;
import com.Classroom_ai.Classroom.auth.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools")
public class TrialController {

    private final TrialGate gate;
    private final UserService userService;

    public TrialController(TrialGate gate, UserService userService) {
        this.gate = gate;
        this.userService = userService;
    }

    /** Open to anyone and read-only: asking does not use up the free try. */
    @GetMapping("/trial")
    public TrialStatusResponse status(HttpServletRequest request) {
        boolean signedIn = userService.findAuthenticatedUser().isPresent();
        return new TrialStatusResponse(signedIn, !signedIn && gate.isAvailable(request));
    }
}
