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

    /**
     * Enable or disable a login. A disabled account is refused at sign-in ("Account is disabled")
     * while its data and history are kept, so this is the reversible alternative to deleting.
     *
     * @param actingUserId the signed-in admin, who may not disable their own login (that would lock
     *                     them straight out of the Logins page).
     */
    public User setActive(String id, boolean active, String actingUserId) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (!active && id.equals(actingUserId)) {
            throw new IllegalArgumentException("You cannot deactivate your own login");
        }
        if (!active && user.getRole() == Role.SUPER_ADMIN && activeSuperAdmins() <= 1) {
            throw new IllegalArgumentException("Cannot deactivate the last active Super Admin");
        }
        user.setActive(active);
        return repo.save(user);
    }

    /** Set a new password for any login (admin-driven reset — the old one is not required). */
    public User resetPassword(String id, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setPasswordHash(encoder.encode(newPassword));
        return repo.save(user);
    }

    private long activeSuperAdmins() {
        return repo.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && u.isActive())
                .count();
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        repo.deleteById(id);
    }
}
