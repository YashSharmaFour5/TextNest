package com.redditclone.backend.payload.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Email(message = "Email should be valid")
    private String email;

    //@Min(value = 0, message = "Age cannot be negative")
    //private Integer age;
    
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth; // Optional, can be used to calculate age

    // You can add other fields here if you decide to make them editable in the future,
    // e.g., a display name or a short bio.
}