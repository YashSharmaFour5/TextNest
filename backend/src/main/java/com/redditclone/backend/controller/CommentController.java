package com.redditclone.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redditclone.backend.exception.ResourceNotFoundException;
import com.redditclone.backend.exception.UnauthorizedException;
import com.redditclone.backend.model.Comment;
import com.redditclone.backend.payload.request.CommentRequest; // Import the new DTO
import com.redditclone.backend.payload.response.MessageResponse;
import com.redditclone.backend.security.services.UserDetailsImpl;
import com.redditclone.backend.service.CommentService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Retrieves all comments for a specific post.
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsForPost(@PathVariable String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUserId = userDetails.getId();
        }

        List<Comment> comments = commentService.getCommentsByPostId(postId, currentUserId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Creates a new comment.
     * @param commentRequest DTO containing content and postId.
     * @return The created Comment object or an error message.
     */
    @PostMapping // This maps to /api/comments
    public ResponseEntity<?> createComment(@RequestBody CommentRequest commentRequest) { // Changed parameter
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();

            // Call the service with the DTO and userId
            Comment createdComment = commentService.createComment(commentRequest, userId); // Updated call
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Deletes a comment by its ID.
     * Accessible only by the comment owner or an admin.
     * @param commentId The ID of the comment to delete.
     * @return A success message or an error message.
     */
    @DeleteMapping("/{commentId}") // Maps to DELETE /api/comments/{commentId}
    public ResponseEntity<?> deleteComment(@PathVariable String commentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String requestingUserId = userDetails.getId();

            commentService.deleteComment(commentId, requestingUserId);
            return ResponseEntity.ok(new MessageResponse("Comment deleted successfully!"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            // Catch any other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error deleting comment: " + e.getMessage()));
        }
    }

}