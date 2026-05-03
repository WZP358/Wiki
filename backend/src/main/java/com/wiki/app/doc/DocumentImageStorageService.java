package com.wiki.app.doc;

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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentImageStorageService {
    private static final long MAX_SIZE_BYTES = 8 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path imageRootDir;

    public DocumentImageStorageService(AppProperties appProperties) {
        this.imageRootDir = Paths.get(appProperties.getDocImageStorageDir()).toAbsolutePath().normalize();
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片大小不能超过 8MB");
        }

        String extension = resolveExtension(file);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = imageRootDir.resolve(filename).normalize();
        if (!target.startsWith(imageRootDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名无效");
        }

        try {
            Files.createDirectories(imageRootDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片上传失败，请稍后重试");
        }
        return "/api/public/doc-images/" + filename;
    }

    public Resource loadImage(String filename) {
        validateFilename(filename);
        Path target = imageRootDir.resolve(filename).normalize();
        if (!target.startsWith(imageRootDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径无效");
        }
        if (!Files.exists(target)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片读取失败");
        }
    }

    public MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
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

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = CONTENT_TYPE_TO_EXT.get(contentType);
        if (extension != null) {
            return extension;
        }

        String originalName = file.getOriginalFilename();
        if (StringUtils.hasText(originalName)) {
            String lower = originalName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return ".jpg";
            }
            if (lower.endsWith(".png")) {
                return ".png";
            }
            if (lower.endsWith(".webp")) {
                return ".webp";
            }
            if (lower.endsWith(".gif")) {
                return ".gif";
            }
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpg/png/webp/gif 图片");
    }

    private void validateFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名不能为空");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名无效");
        }
    }
}
