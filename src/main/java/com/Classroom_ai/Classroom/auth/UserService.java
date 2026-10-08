package com.Classroom_ai.Classroom.auth;

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

    public Optional<User> find(Long id) {
        return userRepository.findById(id);
    }

    // Check if email is already taken
    public boolean isEmailTaken(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public User authenticateUser(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);
    }
}
