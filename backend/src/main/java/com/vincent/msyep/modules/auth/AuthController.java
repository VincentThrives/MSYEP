package com.vincent.msyep.modules.auth;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import com.vincent.msyep.modules.auth.dto.AuthResponse;
import com.vincent.msyep.modules.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok("Login successful", authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<MsyepPrincipal> me(@AuthenticationPrincipal MsyepPrincipal principal) {
        return ApiResponse.ok(principal);
    }
}
