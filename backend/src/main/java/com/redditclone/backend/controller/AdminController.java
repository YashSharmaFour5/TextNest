// src/main/java/com/redditclone/backend/controller/AdminController.java
package com.redditclone.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping; // Import DeleteMapping
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // Import PathVariable
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redditclone.backend.model.User;
import com.redditclone.backend.payload.request.UserRolesUpdateRequest;
import com.redditclone.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // Existing: Endpoint to get a list of all users.
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint to delete a user by their ID.
     * Accessible only by users with the 'ROLE_ADMIN' authority.
     * @param id The ID of the user to delete.
     * @return ResponseEntity with no content if deletion is successful.
     */
    @DeleteMapping("/users/{id}") // DELETE request to /api/admin/users/{id}
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can delete users
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id); // Call the service method to delete the user
        // Return 204 No Content status code for successful deletion
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to update a user's roles by their ID.
     * Accessible only by users with the 'ROLE_ADMIN' authority.
     * @param id The ID of the user whose roles to update.
     * @param request A DTO containing the new set of roles.
     * @return ResponseEntity with the updated User object.
    */
    @PutMapping("/users/{id}/roles") // PUT request to /api/admin/users/{id}/roles
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can update roles
    public ResponseEntity<User> updateUserRoles(@PathVariable String id, @RequestBody UserRolesUpdateRequest request) {
        User updatedUser = userService.updateUserRoles(id, request.getRoles());
        return ResponseEntity.ok(updatedUser);
    }

}