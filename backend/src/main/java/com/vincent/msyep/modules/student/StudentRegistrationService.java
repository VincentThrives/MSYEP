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

/** Student registration: auto MSYEP Register No + Batch Code, and a STUDENT login. */
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

        String rawPassword = input.getPassword();
        String userId = input.getUserId();

        input.setId(null);
        input.setRegisterNo(registerNo);
        input.setBatchCode(batchCode);
        input.setCreatedAt(Instant.now());
        input.setPassword(null); // never persist plaintext

        String loginId = null;
        boolean wantsLogin = StringUtils.hasText(userId) && StringUtils.hasText(rawPassword);
        if (wantsLogin) {
            String email = userId.toLowerCase().trim();
            if (users.existsByEmail(email)) {
                throw new IllegalArgumentException("User ID already in use: " + email);
            }
            loginId = email;
        }

        Student saved = students.save(input);

        if (wantsLogin) {
            User u = users.save(User.builder()
                    .name(saved.getName())
                    .email(loginId)
                    .passwordHash(encoder.encode(rawPassword))
                    .role(Role.STUDENT)
                    .studentId(saved.getId())
                    .centerId(saved.getCenterId())
                    .zoneId(saved.getZoneId())
                    .active(true)
                    .build());
            loginId = u.getEmail();
        }

        String note = wantsLogin
                ? "Student registered with login " + loginId + "."
                : "Student registered (no login credentials provided).";
        return new StudentRegistrationResult(saved, registerNo, batchCode, loginId, note);
    }
}
