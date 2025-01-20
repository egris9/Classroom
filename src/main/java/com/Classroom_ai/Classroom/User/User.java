package com.Classroom_ai.Classroom.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First Name is required")
    private String firstName;

    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String profilePicturePath;  // New field to store the profile picture path

    // Add a constructor with all parameters
    public User(String firstName, String lastName, String email, String password, String profilePicturePath) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.profilePicturePath = profilePicturePath;
    }

    public User() {

    }
}
