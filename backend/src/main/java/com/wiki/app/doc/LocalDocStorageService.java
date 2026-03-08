package com.wiki.app.doc;

import com.wiki.app.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalDocStorageService {
    private final Path rootDir;

    public LocalDocStorageService(AppProperties appProperties) {
        this.rootDir = Paths.get(appProperties.getLocalStorageDir()).toAbsolutePath().normalize();
    }

    public void savePublishedHtml(Long docId, String html) {
        try {
            Files.createDirectories(rootDir);
            Path target = rootDir.resolve(docId + ".html");
            Files.writeString(target, html == null ? "" : html, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // 本地落盘失败不影响主流程。
        }
    }

    public void deleteDocArtifacts(Long docId) {
        try {
            Path target = rootDir.resolve(docId + ".html");
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 清理失败不阻断主流程。
        }
    }
}
