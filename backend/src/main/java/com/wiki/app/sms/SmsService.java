package com.wiki.app.sms;

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
public class SmsService {
    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmsService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean canSendSms() {
        return appProperties.isSmsEnabled() && StringUtils.hasText(appProperties.getSmsGatewayUrl());
    }

    public void sendVerifyCode(String phone, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(appProperties.getSmsApiKey())) {
            headers.set("X-Api-Key", appProperties.getSmsApiKey());
        }

        Map<String, Object> payload = Map.of(
                "phone", phone,
                "code", code,
                "scene", "VERIFY_CODE"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                appProperties.getSmsGatewayUrl(),
                new HttpEntity<>(payload, headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("短信发送失败: " + response.getStatusCode());
        }
    }
}
