package com.vincent.msyep.config;

import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds a Super Admin on first boot so the app is usable out of the box. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.seed.super-admin-email}") String email,
                      @Value("${app.seed.super-admin-password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.countByRole(Role.SUPER_ADMIN) > 0) {
            return;
        }
        User superAdmin = User.builder()
                .name("Super Admin")
                .email(email.toLowerCase().trim())
                .passwordHash(encoder.encode(password))
                .role(Role.SUPER_ADMIN)
                .active(true)
                .build();
        users.save(superAdmin);
        log.info("Seeded SUPER_ADMIN login: {} (change the password after first login)", email);
    }
}
