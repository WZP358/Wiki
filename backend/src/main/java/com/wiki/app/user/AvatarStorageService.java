package com.wiki.app.user;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path avatarRootDir;

    public AvatarStorageService(AppProperties appProperties) {
        this.avatarRootDir = Paths.get(appProperties.getAvatarStorageDir()).toAbsolutePath().normalize();
    }

    public String saveAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please select an avatar file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Avatar size cannot exceed 2MB");
        }

        String contentType = file.getContentType();
        String extension = CONTENT_TYPE_TO_EXT.get(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only jpg/png/webp/gif avatars are supported");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = avatarRootDir.resolve(filename).normalize();
        if (!target.startsWith(avatarRootDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid filename");
        }

        try {
            Files.createDirectories(avatarRootDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Avatar upload failed, please retry");
        }
        return "/api/public/avatars/" + filename;
    }

    public Resource loadAvatar(String filename) {
        validateFilename(filename);
        Path target = avatarRootDir.resolve(filename).normalize();
        if (!target.startsWith(avatarRootDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file path");
        }
        if (!Files.exists(target)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Avatar not found");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Avatar read failed");
        }
    }

    public MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private void validateFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Avatar filename is required");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid filename");
        }
    }
}
