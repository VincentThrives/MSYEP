package com.vincent.msyep.modules.user;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.user.dto.CreateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User create(CreateUserRequest req) {
        String email = req.email().toLowerCase().trim();
        if (repo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }
        User user = User.builder()
                .name(req.name())
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .role(req.role())
                .zoneId(req.zoneId())
                .centerId(req.centerId())
                .studentId(req.studentId())
                .active(true)
                .build();
        return repo.save(user);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        repo.deleteById(id);
    }
}
