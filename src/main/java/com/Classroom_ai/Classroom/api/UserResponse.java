package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.User.User;

public record UserResponse(Long id, String firstName, String lastName, String email, String picture) {

    public static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getProfilePicturePath());
    }
}
