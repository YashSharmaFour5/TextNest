package com.redditclone.backend.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a MultipartFile to Cloudinary.
     * @param file The file to upload.
     * @return The secure URL of the uploaded file.
     * @throws IOException If an error occurs during file upload.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }
        // Upload the file to Cloudinary
        // `resource_type` can be "auto" to let Cloudinary detect, or "image", "video", "raw"
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto"));

        // Return the secure URL
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Deletes a file from Cloudinary using its URL.
     * This method extracts the public ID from the URL.
     * @param fileUrl The secure URL of the file to delete.
     * @throws IOException If an error occurs during deletion.
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            // Extract public ID from the URL.
            // Example: https://res.cloudinary.com/<cloud_name>/image/upload/v1234567890/my_public_id.jpg
            // public_id is "my_public_id"
            String publicId = extractPublicIdFromUrl(fileUrl);
            if (publicId != null && !publicId.isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            // Log the error or handle it as per your application's error handling strategy
            System.err.println("Failed to delete file from Cloudinary: " + fileUrl + ". Error: " + e.getMessage());
        }
    }

    // Helper method to extract public ID from Cloudinary URL
    private String extractPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // Regex to find public ID which is usually the last path segment before file extension
        // e.g., .../upload/v1234567890/public_id.ext
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash != -1 && lastDot != -1 && lastDot > lastSlash) {
            return url.substring(lastSlash + 1, lastDot);
        }
        // Handle cases where there is no extension or specific format
        // This might need more robust parsing depending on your Cloudinary setup
        return null;
    }
}