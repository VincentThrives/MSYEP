package com.vincent.msyep.modules.user;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.user.dto.CreateUserRequest;
import jakarta.validation.Valid;
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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("User deleted", null);
    }
}
