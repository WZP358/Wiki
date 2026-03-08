package com.wiki.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "wiki")
public class AppProperties {
    private String jwtSecret;
    private long jwtExpireHours = 24;
    private long verifyCodeExpireMinutes = 5;
    private long verifyCodeSendIntervalSeconds = 60;
    private long recycleReserveDays = 30;
    private long draftAutoSaveSeconds = 30;
    private String localStorageDir = "./storage/docs";
    private String avatarStorageDir = "./storage/avatars";
    private boolean searchEsEnabled = false;
    private boolean searchEsReindexOnStartup = false;
    private boolean smsEnabled = false;
    private String smsGatewayUrl;
    private String smsApiKey;
    private boolean cloudCleanupEnabled = false;
    private String cloudCleanupUrl;
    private String cloudCleanupApiKey;
}
