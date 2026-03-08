package com.wiki.app.user;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AvatarStorageService avatarStorageService;

    public AuthController(AuthService authService, AvatarStorageService avatarStorageService) {
        this.authService = authService;
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping("/send-code")
    public ApiResponse<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.sendCode(request, IpUtils.resolve(httpRequest)));
    }

    @PostMapping("/send-update-code")
    public ApiResponse<SendCodeResponse> sendUpdateCode(@Valid @RequestBody SendUpdateCodeRequest request,
                                                        HttpServletRequest httpRequest) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.ok(authService.sendUpdateCode(currentUser, request, IpUtils.resolve(httpRequest)));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                               HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.register(request, IpUtils.resolve(httpRequest)));
    }

    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadAvatarResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String avatarUrl = avatarStorageService.saveAvatar(file);
        return ApiResponse.ok(new UploadAvatarResponse(avatarUrl));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, IpUtils.resolve(httpRequest)));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.ok(authService.me(currentUser));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request,
                                                          HttpServletRequest httpRequest) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.ok(authService.updateProfile(currentUser, request, IpUtils.resolve(httpRequest)));
    }
}
