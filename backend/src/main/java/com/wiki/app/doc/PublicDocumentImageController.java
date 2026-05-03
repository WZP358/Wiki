package com.wiki.app.doc;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/doc-images")
public class PublicDocumentImageController {
    private final DocumentImageStorageService imageStorageService;

    public PublicDocumentImageController(DocumentImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = imageStorageService.loadImage(filename);
        MediaType mediaType = imageStorageService.resolveMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
