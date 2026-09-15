package com.vincent.msyep.modules.user;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import com.vincent.msyep.modules.user.dto.CreateUserRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        List<User> users = service.findAll();
        users.forEach(u -> u.setPasswordHash(null)); // never leak hashes
        return ApiResponse.ok(users);
    }

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody CreateUserRequest req) {
        User u = service.create(req);
        u.setPasswordHash(null);
        return ApiResponse.ok("User created", u);
    }

    public record ActiveRequest(boolean active) {}
    public record ResetPasswordRequest(String password) {}

    /** Enable/disable a login — the reversible alternative to deleting it. */
    @PatchMapping("/{id}/active")
    public ApiResponse<User> setActive(@PathVariable String id,
                                       @RequestBody ActiveRequest req,
                                       @AuthenticationPrincipal MsyepPrincipal me) {
        User u = service.setActive(id, req.active(), me == null ? null : me.userId());
        u.setPasswordHash(null);
        return ApiResponse.ok(req.active() ? "Login activated" : "Login deactivated", u);
    }

    /** Admin-driven password reset: set a new password without needing the old one. */
    @PostMapping("/{id}/reset-password")
    public ApiResponse<User> resetPassword(@PathVariable String id,
                                           @RequestBody ResetPasswordRequest req) {
        User u = service.resetPassword(id, req.password());
        u.setPasswordHash(null);
        return ApiResponse.ok("Password updated for " + u.getEmail(), u);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("User deleted", null);
    }
}
