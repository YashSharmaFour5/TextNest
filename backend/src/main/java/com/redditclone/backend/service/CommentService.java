package com.redditclone.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redditclone.backend.model.Comment;
import com.redditclone.backend.model.Post;
import com.redditclone.backend.model.User;
import com.redditclone.backend.payload.request.CommentRequest; // Import the new DTO
import com.redditclone.backend.repository.CommentRepository;
import com.redditclone.backend.repository.PostRepository;
import com.redditclone.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(String postId, String currentUserId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments;
    }

    /**
     * Creates a new comment.
     * @param commentRequest DTO containing comment content and post ID.
     * @param authorId The ID of the user creating the comment.
     * @return The newly created Comment object.
     * @throws RuntimeException if the post or author is not found.
     */
    public Comment createComment(CommentRequest commentRequest, String authorId) { // Updated signature
        // 1. Fetch Post using postId from the request DTO
        Post post = postRepository.findById(commentRequest.getPostId()) // Get postId from DTO
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + commentRequest.getPostId()));

        // 2. Fetch Author (User)
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author (User) not found with ID: " + authorId));

        // 3. Build and save the new Comment
        Comment newComment = Comment.builder()
                .content(commentRequest.getContent()) // Get content from DTO
                .post(post)
                .author(author)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return commentRepository.save(newComment);
    }

    /**
     * Deletes a comment by its ID, after checking authorization.
     * @param commentId The ID of the comment to delete.
     * @param requestingUserId The ID of the user attempting to delete the comment.
     * @throws ResourceNotFoundException if the comment is not found.
     * @throws UnauthorizedException if the requesting user is not the comment owner or an admin.
     */
    @Transactional // Ensure this is transactional for delete operation
    public void deleteComment(String commentId, String requestingUserId) {
        // 1. Find the comment
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        // 2. Authorization Check
        // Get the requesting user to check roles
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("Requesting user not found with ID: " + requestingUserId));

        // Check if the requesting user is an admin
        boolean isAdmin = requestingUser.getRoles().stream()
                                        .anyMatch(role -> role.equals("ROLE_ADMIN"));

        // Check if the requesting user is the comment owner OR an admin
        if (!comment.getAuthor().getId().equals(requestingUserId) && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to delete this comment.");
        }

        // 3. Delete the comment
        commentRepository.delete(comment);
    }    
}