package com.redditclone.backend.payload.request;

import java.util.List; // Add this import for tags

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private List<String> tags; // Include tags if they can be updated
    private boolean isNsfw;    // Include isNsfw if it can be updated
}