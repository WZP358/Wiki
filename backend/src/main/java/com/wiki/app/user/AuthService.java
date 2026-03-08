package com.wiki.app.user;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.mail.VerifyCodeResult;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.JwtTokenProvider;
import com.wiki.app.user.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final VerifyCodeService verifyCodeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;

    public AuthService(UserRepository userRepository,
                       VerifyCodeService verifyCodeService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       SnowflakeIdGenerator idGenerator,
                       OperationLogService operationLogService) {
        this.userRepository = userRepository;
        this.verifyCodeService = verifyCodeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
    }

    public SendCodeResponse sendCode(SendCodeRequest request, String ip) {
        ParsedContact parsedContact = parseContact(request.getTarget());
        VerifyCodeResult result = verifyCodeService.sendCode("register", parsedContact.value(), ip);
        return new SendCodeResponse(result.isTestMode(), result.getCode(), result.getMessage());
    }

    public SendCodeResponse sendUpdateCode(CurrentUser currentUser, SendUpdateCodeRequest request, String ip) {
        UserAccount user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));

        String target = request.getTarget().trim();
        if ("EMAIL".equals(request.getType())) {
            if (!isEmail(target)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Please enter a valid email");
            }
            if (user.getEmail() != null && target.equalsIgnoreCase(user.getEmail())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "New email cannot be the same as current email");
            }
            userRepository.findByEmail(target).ifPresent(exist -> {
                if (!exist.getId().equals(user.getId())) {
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email is already registered");
                }
            });
            VerifyCodeResult result = verifyCodeService.sendCode("bind_email", target, ip);
            return new SendCodeResponse(result.isTestMode(), result.getCode(), result.getMessage());
        }

        if (!isPhone(target)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please enter a valid phone number");
        }
        if (user.getPhone() != null && target.equals(user.getPhone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "New phone cannot be the same as current phone");
        }
        userRepository.findByPhone(target).ifPresent(exist -> {
            if (!exist.getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Phone number is already registered");
            }
        });
        VerifyCodeResult result = verifyCodeService.sendCode("bind_phone", target, ip);
        return new SendCodeResponse(result.isTestMode(), result.getCode(), result.getMessage());
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ip) {
        ParsedContact parsedContact = parseRegisterContact(request);
        verifyCodeService.validate("register", parsedContact.value(), request.getCode());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Username is already taken");
        }
        if (parsedContact.type() == ContactType.EMAIL && userRepository.existsByEmail(parsedContact.value())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email is already registered");
        }
        if (parsedContact.type() == ContactType.PHONE && userRepository.existsByPhone(parsedContact.value())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Phone number is already registered");
        }

        UserAccount user = new UserAccount();
        user.setId(idGenerator.nextId());
        user.setUsername(request.getUsername());
        user.setEmail(parsedContact.type() == ContactType.EMAIL ? parsedContact.value() : null);
        user.setPhone(parsedContact.type() == ContactType.PHONE ? parsedContact.value() : null);
        user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole(UserRole.USER);
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name());
        operationLogService.record(user.getId(), user.getUsername(), "REGISTER", "USER", user.getId().toString(), ip, "User registered");
        return new LoginResponse(token, toProfile(user));
    }

    public LoginResponse login(LoginRequest request, String ip) {
        UserAccount user = userRepository.findByUsernameOrEmailOrPhone(
                        request.getAccount(), request.getAccount(), request.getAccount())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid account or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid account or password");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name());
        operationLogService.record(user.getId(), user.getUsername(), "LOGIN", "USER", user.getId().toString(), ip, "User login");
        return new LoginResponse(token, toProfile(user));
    }

    public UserProfileResponse me(CurrentUser currentUser) {
        UserAccount user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(CurrentUser currentUser, UpdateProfileRequest request, String ip) {
        UserAccount user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        user.setNickname(emptyToNull(request.getNickname()));
        user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));

        if (StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim();
            if (!isEmail(newEmail)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Please enter a valid email");
            }
            if (user.getEmail() == null || !newEmail.equalsIgnoreCase(user.getEmail())) {
                if (!StringUtils.hasText(request.getEmailCode())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification code is required to update email");
                }
                userRepository.findByEmail(newEmail).ifPresent(exist -> {
                    if (!exist.getId().equals(user.getId())) {
                        throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email is already registered");
                    }
                });
                verifyCodeService.validate("bind_email", newEmail, request.getEmailCode().trim());
                user.setEmail(newEmail);
            }
        }

        if (StringUtils.hasText(request.getPhone())) {
            String newPhone = request.getPhone().trim();
            if (!isPhone(newPhone)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Please enter a valid phone number");
            }
            if (user.getPhone() == null || !newPhone.equals(user.getPhone())) {
                if (!StringUtils.hasText(request.getPhoneCode())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification code is required to update phone");
                }
                userRepository.findByPhone(newPhone).ifPresent(exist -> {
                    if (!exist.getId().equals(user.getId())) {
                        throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Phone number is already registered");
                    }
                });
                verifyCodeService.validate("bind_phone", newPhone, request.getPhoneCode().trim());
                user.setPhone(newPhone);
            }
        }
        userRepository.save(user);
        operationLogService.record(user.getId(), user.getUsername(), "UPDATE_PROFILE", "USER", user.getId().toString(), ip, "Update profile");
        return toProfile(user);
    }

    private ParsedContact parseRegisterContact(RegisterRequest request) {
        if (StringUtils.hasText(request.getContact())) {
            return parseContact(request.getContact());
        }
        if (StringUtils.hasText(request.getEmail()) && StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Use one contact only: email or phone");
        }
        if (StringUtils.hasText(request.getEmail())) {
            return parseContact(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            return parseContact(request.getPhone());
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Email or phone is required");
    }

    private ParsedContact parseContact(String rawContact) {
        if (!StringUtils.hasText(rawContact)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Contact is required");
        }
        String value = rawContact.trim();
        if (isPhone(value)) {
            return new ParsedContact(ContactType.PHONE, value);
        }
        if (isEmail(value)) {
            return new ParsedContact(ContactType.EMAIL, value);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Please enter a valid email or phone number");
    }

    private boolean isEmail(String value) {
        return value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isPhone(String value) {
        return value.matches("^\\+?[0-9]{6,20}$");
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserProfileResponse toProfile(UserAccount user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .build();
    }

    private enum ContactType {
        EMAIL,
        PHONE
    }

    private record ParsedContact(ContactType type, String value) {
    }
}