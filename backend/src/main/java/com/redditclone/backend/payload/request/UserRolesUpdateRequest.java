package com.redditclone.backend.payload.request;

import java.util.Set;

import lombok.Data;

@Data
public class UserRolesUpdateRequest {
    private Set<String> roles; // A set to hold the new roles (e.g., "ROLE_USER", "ROLE_ADMIN")
}
