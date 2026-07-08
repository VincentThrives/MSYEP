package com.vincent.msyep.modules.auth;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import com.vincent.msyep.modules.auth.dto.AuthResponse;
import com.vincent.msyep.modules.auth.dto.LoginRequest;
import com.vincent.msyep.modules.auth.dto.OtpDtos.OtpRequest;
import com.vincent.msyep.modules.auth.dto.OtpDtos.OtpRequestResult;
import com.vincent.msyep.modules.auth.dto.OtpDtos.OtpVerify;
import com.vincent.msyep.modules.auth.dto.StudentSelfRegister;
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

    @PostMapping("/register")
    public ApiResponse<StudentSelfRegister.Result> register(@Valid @RequestBody StudentSelfRegister.Request req) {
        StudentSelfRegister.Result result = authService.registerStudent(req);
        return ApiResponse.ok(result.message(), result);
    }

    @PostMapping("/otp/request")
    public ApiResponse<OtpRequestResult> requestOtp(@Valid @RequestBody OtpRequest req) {
        OtpRequestResult result = authService.requestOtp(req.identifier());
        return ApiResponse.ok(result.message(), result);
    }

    @PostMapping("/otp/verify")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerify req) {
        return ApiResponse.ok("Login successful", authService.verifyOtp(req.identifier(), req.otp()));
    }

    @GetMapping("/me")
    public ApiResponse<MsyepPrincipal> me(@AuthenticationPrincipal MsyepPrincipal principal) {
        return ApiResponse.ok(principal);
    }
}
