package com.redditclone.backend.payload.response;

import java.time.LocalDate; // Import for LocalDate
import java.time.LocalDateTime;
import java.time.Period; // Import for Period to calculate age
import java.util.Set;

import com.redditclone.backend.model.User; // Import your User model

import lombok.Data;
// Removed @AllArgsConstructor and @NoArgsConstructor as we'll use a custom constructor

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
public class UserProfileResponse {
    private String id;
    private String username;
    private String email;
    private LocalDate dateOfBirth; // Include dateOfBirth if you want to expose it in the response
    private Integer age;         // Calculated age for response
    private boolean isAdult;     // Calculated isAdult for response
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> roles;

    // Custom constructor to map User entity to this DTO and calculate derived fields
    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.dateOfBirth = user.getDateOfBirth(); // Copy dateOfBirth from the User object
        this.roles = user.getRoles();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();

        // Calculate age and isAdult based on dateOfBirth
        if (user.getDateOfBirth() != null) {
            this.age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
            // Define your adult age threshold here (e.g., 18)
            this.isAdult = this.age >= 18;
        } else {
            // Handle cases where dateOfBirth might be null (e.g., old user data or validation failure)
            this.age = null;
            this.isAdult = false; // Default to false if DOB is unknown
        }
    }
}