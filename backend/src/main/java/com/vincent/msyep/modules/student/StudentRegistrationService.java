package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.CounterService;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Student registration: auto MSYEP Register No + Batch Code, and an OTP-only STUDENT login. */
@Service
public class StudentRegistrationService {

    private final StudentRepository students;
    private final UserRepository users;
    private final CounterService counters;
    private final PasswordEncoder encoder;

    public StudentRegistrationService(StudentRepository students, UserRepository users,
                                      CounterService counters, PasswordEncoder encoder) {
        this.students = students;
        this.users = users;
        this.counters = counters;
        this.encoder = encoder;
    }

    /** Register without an explicitly chosen password (admin-created students). */
    public StudentRegistrationResult register(Student input) {
        return register(input, null);
    }

    /**
     * @param chosenPassword the password the student picked during self-registration. When absent we
     *                       fall back to any password carried on the record, and failing that store a
     *                       random unusable hash (the account then has no way to sign in until a
     *                       password is set).
     */
    public StudentRegistrationResult register(Student input, String chosenPassword) {
        if (!StringUtils.hasText(input.getName())) {
            throw new IllegalArgumentException("Student name is required");
        }

        int year = LocalDate.now().getYear();
        long seq = counters.next("student");
        String registerNo = String.format("MSYEP%d%06d", year, seq);
        String batchCode = String.format("BATCH-%d-%03d", year, seq);

        // Students now sign in with a password they choose; only the hash is ever stored.
        String rawPassword = StringUtils.hasText(chosenPassword) ? chosenPassword : input.getPassword();
        String userId = input.getUserId();
        input.setId(null);
        input.setRegisterNo(registerNo);
        input.setBatchCode(batchCode);
        input.setCreatedAt(Instant.now());
        input.setPassword(null); // never persist plaintext

        // The login key (User.email) is the given User ID, else the student's email, else phone.
        String loginId = firstNonBlank(userId, input.getEmail(), input.getPhone());
        boolean wantsLogin = StringUtils.hasText(loginId);
        if (wantsLogin) {
            loginId = loginId.toLowerCase().trim();
            if (users.existsByEmail(loginId)) {
                throw new IllegalArgumentException("User ID already in use: " + loginId);
            }
        }

        Student saved = students.save(input);

        if (wantsLogin) {
            User u = users.save(User.builder()
                    .name(saved.getName())
                    .email(loginId)
                    // The chosen password, or a random unusable hash when none was supplied.
                    .passwordHash(encoder.encode(StringUtils.hasText(rawPassword)
                            ? rawPassword : UUID.randomUUID().toString()))
                    .role(Role.STUDENT)
                    .studentId(saved.getId())
                    .centerId(saved.getCenterId())
                    .zoneId(saved.getZoneId())
                    .active(true)
                    .build());
            loginId = u.getEmail();
        }

        String note = wantsLogin
                ? "Student registered. Sign in as " + loginId + " with the chosen password."
                : "Student registered (no email/mobile provided — login unavailable).";
        return new StudentRegistrationResult(saved, registerNo, batchCode, loginId, note);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
