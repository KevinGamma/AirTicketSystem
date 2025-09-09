package com.airticket.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/{category}/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String category, @PathVariable String filename) {
        try {
            System.out.println("FileController: Requesting file - Category: " + category + ", Filename: " + filename);
            System.out.println("FileController: Upload dir: " + uploadDir);
            
            Path filePath = Paths.get(uploadDir, category, filename);
            System.out.println("FileController: Full path: " + filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());
            System.out.println("FileController: Resource exists: " + resource.exists() + ", readable: " + resource.isReadable());

            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                
                // Determine content type based on file extension
                String fileExtension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                switch (fileExtension) {
                    case "jpg":
                    case "jpeg":
                        contentType = "image/jpeg";
                        break;
                    case "png":
                        contentType = "image/png";
                        break;
                    case "gif":
                        contentType = "image/gif";
                        break;
                }

                System.out.println("FileController: Serving file with content type: " + contentType);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                System.out.println("FileController: File not found or not readable");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            System.out.println("FileController: Exception occurred: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}