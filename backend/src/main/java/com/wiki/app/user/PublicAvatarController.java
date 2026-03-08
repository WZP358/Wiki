package com.wiki.app.user;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/avatars")
public class PublicAvatarController {
    private final AvatarStorageService avatarStorageService;

    public PublicAvatarController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Resource resource = avatarStorageService.loadAvatar(filename);
        MediaType mediaType = avatarStorageService.resolveMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
