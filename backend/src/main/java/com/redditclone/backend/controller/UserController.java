package com.redditclone.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redditclone.backend.payload.request.UserProfileUpdateRequest; // Assuming you have this DTO for updates
import com.redditclone.backend.payload.response.UserProfileResponse; // Assuming you have this DTO
import com.redditclone.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users") // Base mapping for user-related endpoints
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Inject your UserService

    /**
     * Endpoint to get the currently authenticated user's profile.
     * Accessible only to authenticated users.
     * @return ResponseEntity with the user's profile data.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Ensures only logged-in users can access
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        UserProfileResponse userProfile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Endpoint to update the currently authenticated user's profile.
     * Accessible only to authenticated users.
     * @param updateRequest The update request body.
     * @return ResponseEntity with the updated user's profile data.
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Ensures only logged-in users can update
    public ResponseEntity<UserProfileResponse> updateUserProfile(@Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        UserProfileResponse updatedProfile = userService.updateCurrentUserProfile(updateRequest);
        return ResponseEntity.ok(updatedProfile);
    }
}