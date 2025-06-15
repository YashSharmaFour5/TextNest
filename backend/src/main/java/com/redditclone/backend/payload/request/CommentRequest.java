package com.redditclone.backend.payload.request;

import jakarta.validation.constraints.NotBlank; // Use jakarta.validation for Spring Boot 3+
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    @NotBlank(message = "Comment content cannot be empty")
    private String content;

    @NotBlank(message = "Post ID cannot be empty")
    private String postId; // To link the comment to a specific post
}