package com.Classroom_ai.Classroom.User;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;


import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5175")
public class UserController {

    private final UserService userService;

    // Constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestParam String firstName,
                                          @RequestParam String lastName,
                                          @RequestParam String email,
                                          @RequestParam String password,
                                          @RequestParam(required = false) MultipartFile profilePicture) {
        try {
            // Logic to handle profile picture
            String profilePicturePath = "default-profile.png"; // Default image if none provided
            if (profilePicture  != null && !profilePicture.isEmpty()) {
                profilePicturePath  = saveProfilePicture(profilePicture);
            }

            // Create and save the user
            User user = new User(firstName, lastName, email, password, profilePicturePath);
            User savedUser = userService.registerUser(user);

            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Email already exists or error occurred!");
        }
    }

    private String saveProfilePicture(MultipartFile file) throws IOException {
        // Générer un nom de fichier unique pour éviter les collisions
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // Définir le chemin du dossier où les images doivent être stockées
        String uploadDir = "src/main/resources/static/uploads/";  // Utilisez un répertoire "static/uploads" à la racine du projet

        // Créer le répertoire s'il n'existe pas
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists() && !uploadDirFile.mkdirs()) {
            throw new IOException("Failed to create directory: " + uploadDir);
        }

        // Définir le chemin de destination du fichier
        Path path = Paths.get(uploadDir + fileName);

        // Copier le fichier dans le répertoire de destination
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // Retourner le nom du fichier (ou son chemin relatif)
        return "/uploads/" + fileName;  // Retourner le chemin relatif pour l'URL
    }

    // Sign-In Endpoint
    @PostMapping("/signing")
    public ResponseEntity<?> loginUser(@RequestParam String email, @RequestParam String password) {
        User user = userService.authenticateUser(email, password);
        if (user != null) {
            // Générer le token JWT
            String token = JwtTokenUtil.generateToken(user);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);  // Envoyer le token JWT au frontend
            response.put("firstname", user.getFirstName().toUpperCase());  // Utiliser getFirstName()
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials!"); // Failure
        }
    }

}
