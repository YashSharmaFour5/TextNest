package com.redditclone.backend.model;

import java.time.Instant; // Recommended for timestamps

import org.springframework.data.annotation.CreatedDate; // Import for automatic creation timestamp
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate; // Import for automatic modification timestamp
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder; // Added for builder pattern
import lombok.Data;
import lombok.NoArgsConstructor; // Added for no-arg constructor

@Data // Generates getters, setters, toString, equals, hashCode
@AllArgsConstructor // Generates a constructor with all fields
@NoArgsConstructor // Generates a no-argument constructor (required by JPA/Spring Data)
@Builder // Generates a builder pattern for creating instances
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @DBRef // Reference to the Post document
    private Post post;

    @DBRef // Reference to the User document (author of the comment)
    private User author;

    private String content;

    @CreatedDate // Spring Data MongoDB will automatically set this on creation
    private Instant createdAt;

    @LastModifiedDate // Spring Data MongoDB will automatically update this on modification
    private Instant updatedAt;

    // The author's username is provided dynamically by the getAuthorUsername() method.

    // Custom getter to return the author's username
    // This will be used when Spring converts the Comment object to JSON
    // for the frontend.
    public String getAuthorUsername() {
        return author != null ? author.getUsername() : "Unknown";
    }

    // You can remove your manual constructor and setUpdatedAt method
    // because @CreatedDate and @LastModifiedDate with auditing will handle them.
}