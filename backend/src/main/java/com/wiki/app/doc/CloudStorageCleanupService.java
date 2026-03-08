package com.wiki.app.doc;

import com.wiki.app.config.AppProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CloudStorageCleanupService {
    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public CloudStorageCleanupService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void cleanupDoc(Long docId) {
        if (!appProperties.isCloudCleanupEnabled() || !StringUtils.hasText(appProperties.getCloudCleanupUrl())) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(appProperties.getCloudCleanupApiKey())) {
            headers.set("X-Api-Key", appProperties.getCloudCleanupApiKey());
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                appProperties.getCloudCleanupUrl(),
                new HttpEntity<>(Map.of("docId", docId), headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("云存储清理失败: " + response.getStatusCode());
        }
    }
}
