package com.Classroom_ai.Classroom.api;

import com.Classroom_ai.Classroom.auth.User;

/** {@code picture} is the path to fetch the picture from, or null when the user has none. */
public record UserResponse(Long id, String firstName, String lastName, String email, String picture) {

    public static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), pictureUrl(user));
    }

    public static String pictureUrl(User user) {
        String key = user.getProfilePicturePath();
        return key == null || key.isBlank() ? null : "/api/users/" + user.getId() + "/picture";
    }
}
