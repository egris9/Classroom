package com.Classroom_ai.Classroom.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5175")
public class UserController {

    private final UserService userService;



    // Constructor injection
    public UserController(UserService userService,UserRepository userRepository) {
        this.userService = userService;

    }

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestParam String firstName,
                                          @RequestParam String lastName,
                                          @RequestParam String email,
                                          @RequestParam String password,
                                          @RequestParam(required = false) MultipartFile profilePicture) {
        try {
            if (userService.isEmailTaken(email)) {
                return ResponseEntity.status(400).body("Email is already taken.");
            }

            String profilePicturePath = "default-profile.png";
            if (profilePicture != null && !profilePicture.isEmpty()) {
                profilePicturePath = saveProfilePicture(profilePicture);
            }

            User user = new User(firstName, lastName, email, password, profilePicturePath);
            User savedUser = userService.registerUser(user);

            return ResponseEntity.ok(savedUser);

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("Error occurred while saving profile picture.");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error occurred during registration.");
        }



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






    /*private String saveProfilePicture(MultipartFile file) throws IOException {
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
    }*/

    // Sign-In Endpoint
    @PostMapping("/signin")
    public ResponseEntity<?> loginUser(@RequestParam String email, @RequestParam String password) {
        User user = userService.authenticateUser(email, password);
        if (user != null) {
            // Generate JWT token
            String token = JwtTokenUtil.generateToken(user);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("firstName", user.getFirstName());  // Return first name as part of the response
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials!");
        }
    }
}
