package com.Classroom_ai.Classroom.User;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Constructor injection
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new user
    public User registerUser(User user) {
        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }



    public User getAuthenticatedUser() {
        // Retrieve the user ID from the SecurityContext
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // Fetch the user by ID
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userEmail));
    }




    // Check if email is already taken
    public boolean isEmailTaken(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    // Authenticate user (login)
    public User authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            if (passwordMatches) {
                return user; // Return the authenticated user
            }
        }
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword())) // Check password
                .orElseThrow(() -> new RuntimeException("Invalid email or password")); // Handle authentication failure
    }

}
