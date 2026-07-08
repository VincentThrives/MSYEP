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

    public StudentRegistrationResult register(Student input) {
        if (!StringUtils.hasText(input.getName())) {
            throw new IllegalArgumentException("Student name is required");
        }

        int year = LocalDate.now().getYear();
        long seq = counters.next("student");
        String registerNo = String.format("MSYEP%d%06d", year, seq);
        String batchCode = String.format("BATCH-%d-%03d", year, seq);

        // Students authenticate by OTP only — no password is ever collected or stored.
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
                    // Random unusable hash — student logins go through OTP, never a password.
                    .passwordHash(encoder.encode(UUID.randomUUID().toString()))
                    .role(Role.STUDENT)
                    .studentId(saved.getId())
                    .centerId(saved.getCenterId())
                    .zoneId(saved.getZoneId())
                    .active(true)
                    .build());
            loginId = u.getEmail();
        }

        String note = wantsLogin
                ? "Student registered. OTP login enabled for " + loginId + "."
                : "Student registered (no email/mobile provided — OTP login unavailable).";
        return new StudentRegistrationResult(saved, registerNo, batchCode, loginId, note);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
