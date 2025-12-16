package com.example.dat.dependent.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping
public class DependentPhotoController {

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    @GetMapping("/dependents/{filename:.+}")
    public ResponseEntity<Resource> serveDependentPhoto(@PathVariable String filename) {
        try {
            Path uploadPath = Paths.get(baseUploadDir).toAbsolutePath();
            Path file = uploadPath.resolve("dependents").resolve(filename);
            if (!Files.exists(file) || Files.isDirectory(file)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new PathResource(file);

            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
