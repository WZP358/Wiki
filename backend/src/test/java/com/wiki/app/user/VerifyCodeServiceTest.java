package com.wiki.app.user;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.config.AppProperties;
import com.wiki.app.mail.MailService;
import com.wiki.app.mail.VerifyCodeResult;
import com.wiki.app.sms.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MailService mailService;
    @Mock
    private SmsService smsService;

    private AppProperties properties;
    private VerifyCodeService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setVerifyCodeExpireMinutes(5);
        properties.setVerifyCodeSendIntervalSeconds(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new VerifyCodeService(redisTemplate, properties, mailService, smsService, "");
    }

    @Test
    void sendCodeStoresCodeAndReturnsDemoModeWhenNoProviderConfigured() {
        when(valueOperations.setIfAbsent(eq("verify:limit:register:ip:127.0.0.1"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(eq("verify:limit:register:target:alice@example.com"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        VerifyCodeResult result = service.sendCode("register", "alice@example.com", "127.0.0.1");

        assertThat(result.isTestMode()).isTrue();
        assertThat(result.getCode()).matches("\\d{6}");
        verify(valueOperations).set(eq("verify:code:register:alice@example.com"), eq(result.getCode()), any(Duration.class));
    }

    @Test
    void sendCodeRateLimitsSameIpOrTarget() {
        when(valueOperations.setIfAbsent(eq("verify:limit:register:ip:127.0.0.1"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.sendCode("register", "alice@example.com", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void validateDeletesMatchingCodeAndRejectsMismatch() {
        when(valueOperations.get("verify:code:register:alice@example.com")).thenReturn("123456");
        service.validate("register", "alice@example.com", "123456");
        verify(redisTemplate).delete("verify:code:register:alice@example.com");

        when(valueOperations.get("verify:code:register:bob@example.com")).thenReturn("111111");
        assertThatThrownBy(() -> service.validate("register", "bob@example.com", "222222"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.VERIFY_CODE_INVALID);
    }
}
