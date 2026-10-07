package com.Classroom_ai.Classroom.auth;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.api.SigninRequest;
import com.Classroom_ai.Classroom.api.SigninResponse;
import com.Classroom_ai.Classroom.api.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5175")
public class UserController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    public UserController(UserService userService, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> registerUser(@RequestParam String firstName,
                                                     @RequestParam String lastName,
                                                     @RequestParam String email,
                                                     @RequestParam String password,
                                                     @RequestParam(required = false) MultipartFile profilePicture)
            throws IOException {
        if (userService.isEmailTaken(email)) {
            throw new EmailAlreadyTakenException();
        }

        String profilePicturePath = "default-profile.png";
        if (profilePicture != null && !profilePicture.isEmpty()) {
            profilePicturePath = saveProfilePicture(profilePicture);
        }

        User saved = userService.registerUser(new User(firstName, lastName, email, password, profilePicturePath));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(saved));
    }

    @GetMapping("/profile-picture/{fileName}")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            // Check if file exists and is readable
            if (!Files.exists(filePath)) {
                // If file not found, try to serve default image
                filePath = Paths.get(uploadDir).resolve("default-profile.png");
                if (!Files.exists(filePath)) {
                    return ResponseEntity.notFound().build();
                }
            }
            byte[] imageBytes = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String saveProfilePicture(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown-file";
        }

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + "-" +
                StringUtils.cleanPath(originalFilename);

        // Save file
        Path targetLocation = Paths.get(uploadDir).resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @PostMapping("/signin")
    public SigninResponse signin(@RequestBody @Valid SigninRequest request) {
        User user = userService.authenticateUser(request.email(), request.password());
        return new SigninResponse(jwtTokenUtil.generateToken(user), UserResponse.of(user));
    }
}
