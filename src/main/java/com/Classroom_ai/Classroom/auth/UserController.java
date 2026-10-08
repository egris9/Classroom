package com.Classroom_ai.Classroom.auth;

import com.Classroom_ai.Classroom.api.SigninRequest;
import com.Classroom_ai.Classroom.api.SigninResponse;
import com.Classroom_ai.Classroom.api.UserResponse;
import com.Classroom_ai.Classroom.material.Materials;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5175")
public class UserController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final Materials materials;

    public UserController(UserService userService, JwtTokenUtil jwtTokenUtil, Materials materials) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.materials = materials;
    }

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

        String pictureKey = null;
        if (profilePicture != null && !profilePicture.isEmpty()) {
            pictureKey = materials.putPicture(profilePicture);
        }

        User saved = userService.registerUser(new User(firstName, lastName, email, password, pictureKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(saved));
    }

    @PostMapping("/signin")
    public SigninResponse signin(@RequestBody @Valid SigninRequest request) {
        User user = userService.authenticateUser(request.email(), request.password());
        return new SigninResponse(jwtTokenUtil.generateToken(user), UserResponse.of(user));
    }
}
