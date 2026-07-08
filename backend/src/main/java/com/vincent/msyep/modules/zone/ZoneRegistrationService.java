package com.vincent.msyep.modules.zone;

import com.vincent.msyep.common.CounterService;
import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import com.vincent.msyep.modules.zone.dto.ZoneRegistrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Franchise / zone sign-up: auto code, ZONE login, Pending status. */
@Service
public class ZoneRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ZoneRegistrationService.class);

    private static final Map<String, Integer> TIER_AMOUNT = Map.of(
            "Silver", 75000, "Gold", 100000, "Platinum", 125000, "Diamond", 150000);

    private final ZoneRepository zones;
    private final UserRepository users;
    private final CounterService counters;
    private final PasswordEncoder encoder;

    public ZoneRegistrationService(ZoneRepository zones, UserRepository users,
                                   CounterService counters, PasswordEncoder encoder) {
        this.zones = zones;
        this.users = users;
        this.counters = counters;
        this.encoder = encoder;
    }

    public ZoneRegistrationResult register(Zone input) {
        if (!StringUtils.hasText(input.getName()) && !StringUtils.hasText(input.getOrganizationName())) {
            throw new IllegalArgumentException("Organization / Zone name is required");
        }
        if (!StringUtils.hasText(input.getName())) {
            input.setName(input.getOrganizationName());
        }

        int year = LocalDate.now().getYear();
        long seq = counters.next("zone");
        String code = String.format("ZONE-%d-%04d", year, seq);

        String rawPassword = input.getPassword();
        String userId = input.getUserId();

        input.setId(null);
        input.setCode(code);
        input.setRegistrationDate(LocalDate.now().toString());
        if (input.getMembershipTier() != null) {
            input.setMembershipAmount(TIER_AMOUNT.get(input.getMembershipTier()));
        }
        if (!StringUtils.hasText(input.getStatus())) input.setStatus("PENDING");
        input.setCreatedAt(Instant.now());
        input.setPassword(null); // never persist the plaintext password

        String loginId = null;
        boolean wantsLogin = StringUtils.hasText(userId) && StringUtils.hasText(rawPassword);
        if (wantsLogin) {
            String email = userId.toLowerCase().trim();
            if (users.existsByEmail(email)) {
                throw new IllegalArgumentException("User ID already in use: " + email);
            }
            loginId = email;
        }

        Zone saved = zones.save(input);

        if (wantsLogin) {
            users.save(User.builder()
                    .name(StringUtils.hasText(saved.getOwnerName()) ? saved.getOwnerName() : saved.getName())
                    .email(loginId)
                    .passwordHash(encoder.encode(rawPassword))
                    .role(Role.ZONE)
                    .zoneId(saved.getId())
                    .active(true)
                    .build());
        }

        String note = wantsLogin
                ? "Zone registered (status " + saved.getStatus()
                    + "). Payment + activation email are pending integration."
                : "Zone registered (no login credentials provided).";
        return new ZoneRegistrationResult(saved, code, loginId, saved.getStatus(),
                saved.getMembershipAmount(), note);
    }
}
