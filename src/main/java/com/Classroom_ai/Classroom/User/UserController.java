package com.Classroom_ai.Classroom.User;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;



import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    // Constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }
    }

    // Sign-In Endpoint
    @PostMapping("/signing")
    public ResponseEntity<?> loginUser(@RequestParam String email, @RequestParam String password) {
        User user = userService.authenticateUser(email, password);
        if (user != null) {
            return ResponseEntity.ok(user); // Success
        } else {
            return ResponseEntity.status(401).body("Invalid credentials!"); // Failure
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Invalidate the session
            request.getSession().invalidate();
            // Clear the authentication
            SecurityContextHolder.clearContext();
            // You can also add custom logic to respond after logout
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during logout");
        }
    }

    @GetMapping("/user")
    public Map<String, String> getUserInfo(Authentication authentication) {
        Map<String, String> userInfo = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal(); // Get the authenticated user
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("email", user.getEmail());
        }
        return userInfo;
    }

}
