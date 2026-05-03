package com.wiki.app.user;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.config.AppProperties;
import com.wiki.app.mail.MailService;
import com.wiki.app.mail.VerifyCodeResult;
import com.wiki.app.sms.SmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class VerifyCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final MailService mailService;
    private final SmsService smsService;
    private final String mailUsername;

    public VerifyCodeService(StringRedisTemplate redisTemplate,
                             AppProperties appProperties,
                             MailService mailService,
                             SmsService smsService,
                             @Value("${spring.mail.username:}") String mailUsername) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
        this.mailService = mailService;
        this.smsService = smsService;
        this.mailUsername = mailUsername;
    }

    public VerifyCodeResult sendCode(String scene, String target, String ip) {
        Duration interval = Duration.ofSeconds(appProperties.getVerifyCodeSendIntervalSeconds());
        checkRateLimit("verify:limit:" + scene + ":ip:" + ip, interval);
        checkRateLimit("verify:limit:" + scene + ":target:" + target, interval);

        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        redisTemplate.opsForValue().set(codeKey(scene, target), code, Duration.ofMinutes(appProperties.getVerifyCodeExpireMinutes()));

        VerifyCodeResult result = new VerifyCodeResult();
        result.setCode(code);

        if (isEmail(target) && StringUtils.hasText(mailUsername) && mailService.canSendMail(mailUsername)) {
            try {
                mailService.sendVerifyCode(mailUsername, target, code);
                result.setTestMode(false);
                result.setMessage("验证码已发送，当前演示环境已同步返回验证码");
                return result;
            } catch (Exception ignored) {
                // Fallback to demo mode when the external provider is unavailable.
            }
        }

        if (isPhone(target) && smsService.canSendSms()) {
            try {
                smsService.sendVerifyCode(target, code);
                result.setTestMode(false);
                result.setMessage("验证码已发送，当前演示环境已同步返回验证码");
                return result;
            } catch (Exception ignored) {
                // Fallback to demo mode when the external provider is unavailable.
            }
        }

        result.setTestMode(true);
        result.setMessage("当前为演示模式，验证码已直接返回");
        return result;
    }

    public void validate(String scene, String target, String code) {
        String key = codeKey(scene, target);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null || !cached.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_INVALID, "验证码错误或已过期");
        }
        redisTemplate.delete(key);
    }

    private String codeKey(String scene, String target) {
        return "verify:code:" + scene + ":" + target;
    }

    private boolean isEmail(String target) {
        return target.contains("@");
    }

    private boolean isPhone(String target) {
        return target.matches("^\\+?[0-9]{6,20}$");
    }

    private void checkRateLimit(String key, Duration duration) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", duration);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "同一 IP 或账号 1 分钟内最多请求 1 次验证码");
        }
    }
}
