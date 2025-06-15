package com.redditclone.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    private String title;
    private String content; // Optional text content
    private List<String> mediaUrls = new ArrayList<>(); // URLs from Cloudinary
    @DBRef // Reference to the User document
    private User author; // Stores the actual User object, not just ID
    private List<String> tags = new ArrayList<>();
    private boolean isNsfw; // Is 18+ content
    private List<String> likedBy = new ArrayList<>(); // User IDs who liked this post
    private int likeCount; // Denormalized count for quick display
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.likeCount = 0;
    }

    // Update timestamp before saving
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method to add a like
    public void addLike(String userId) {
        if (!likedBy.contains(userId)) {
            likedBy.add(userId);
            this.likeCount = likedBy.size();
        }
    }

    // Helper method to remove a like
    public void removeLike(String userId) {
        if (likedBy.remove(userId)) {
            this.likeCount = likedBy.size();
        }
    }
}