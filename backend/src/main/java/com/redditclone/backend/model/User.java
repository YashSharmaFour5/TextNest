package com.redditclone.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
//import lombok.NoArgsConstructor;

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@Document(collection = "users") // MongoDB document mapping
public class User {
    @Id // Marks this field as the document's ID
    private String id;
    private String username;
    private String email;
    private String password; // Hashed password
    private LocalDate dateOfBirth; // Used to calculate age
    @Transient
    private Integer age;
    @Transient
    private boolean isAdult; // Derived from age, or can be set explicitly
    private Set<String> roles = new HashSet<>(); // e.g., "ROLE_USER", "ROLE_ADMIN"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor to set creation timestamp
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Pre-save update timestamp
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Method to check if user has a specific role
    public boolean hasRole(String roleName) {
        return roles.contains(roleName);
    }
}