package com.Classroom_ai.Classroom.User;


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


    public User registerUser(User user) {
        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Authenticate user
    public User authenticateUser(String email, String password) {
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByEmail(email));

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            if (passwordMatches) {
                return user;
            }
        }
        return null;
    }
}

