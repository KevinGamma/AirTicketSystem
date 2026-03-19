package com.airticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    public String storeFile(MultipartFile file, String category) throws IOException {
        Path uploadPath = Paths.get(uploadDir, category);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return baseUrl + "/api/files/" + category + "/" + fileName;
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith(baseUrl)) {
                String relativePath = fileUrl.replace(baseUrl + "/api/files/", "");

                String[] pathParts = relativePath.split("/");
                Path filePath = Paths.get(uploadDir, pathParts);
                Files.deleteIfExists(filePath);
            }
        } catch (Exception e) {
            com.airticket.util.AppLog.error("Failed to delete file: " + fileUrl);
        }
    }

    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && 
               (contentType.equals("image/jpeg") || 
                contentType.equals("image/jpg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/gif"));
    }

    public boolean isFileSizeValid(MultipartFile file, long maxSizeInMB) {
        return file.getSize() <= maxSizeInMB * 1024 * 1024;
    }
}