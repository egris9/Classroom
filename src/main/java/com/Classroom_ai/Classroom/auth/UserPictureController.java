package com.Classroom_ai.Classroom.auth;

import com.Classroom_ai.Classroom.User.User;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredFileMissingException;
import com.Classroom_ai.Classroom.material.StoredPicture;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Open to anyone: an {@code <img>} tag cannot send an Authorization header. */
@RestController
@RequestMapping("/api/users")
public class UserPictureController {

    private final UserService userService;
    private final Materials materials;

    public UserPictureController(UserService userService, Materials materials) {
        this.userService = userService;
        this.materials = materials;
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<Resource> picture(@PathVariable Long id) {
        User user = userService.find(id).orElseThrow(StoredFileMissingException::new);
        StoredPicture picture = materials.openPicture(user.getProfilePicturePath());
        return ResponseEntity.ok().contentType(picture.type()).body(picture.resource());
    }
}
