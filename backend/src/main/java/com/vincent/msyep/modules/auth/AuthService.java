package com.vincent.msyep.modules.auth;

import com.vincent.msyep.config.security.JwtUtil;
import com.vincent.msyep.modules.auth.dto.AuthResponse;
import com.vincent.msyep.modules.auth.dto.LoginRequest;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwt.generate(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.getZoneId(), user.getCenterId(), user.getStudentId());
    }
}
