package com.vincent.msyep.modules.auth;

import com.vincent.msyep.config.security.JwtUtil;
import com.vincent.msyep.modules.auth.dto.AuthResponse;
import com.vincent.msyep.modules.auth.dto.LoginRequest;
import com.vincent.msyep.modules.auth.dto.OtpDtos.OtpRequestResult;
import com.vincent.msyep.modules.auth.dto.StudentSelfRegister;
import com.vincent.msyep.modules.student.Student;
import com.vincent.msyep.modules.student.StudentRegistrationService;
import com.vincent.msyep.modules.student.StudentRepository;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final StudentRepository students;
    private final StudentRegistrationService studentRegistration;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final OtpService otp;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public AuthService(UserRepository users, StudentRepository students,
                       StudentRegistrationService studentRegistration, PasswordEncoder encoder,
                       JwtUtil jwt, OtpService otp, Optional<JavaMailSender> mailSender,
                       @Value("${spring.mail.username:}") String mailFrom) {
        this.users = users;
        this.students = students;
        this.studentRegistration = studentRegistration;
        this.encoder = encoder;
        this.jwt = jwt;
        this.otp = otp;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Password login — for Super Admin / Admin / Zone / Center / Staff / Finance (NOT students). */
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (user.getRole() == Role.STUDENT) {
            throw new BadCredentialsException("Students must sign in with OTP");
        }
        if (!user.isActive()) throw new BadCredentialsException("Account is disabled");
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return toResponse(user);
    }

    /** Public student self-registration — creates the student and an OTP-only login. */
    public StudentSelfRegister.Result registerStudent(StudentSelfRegister.Request req) {
        String phone = req.phone().trim();
        if (students.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("A student is already registered with mobile " + phone);
        }
        String email = req.email() == null ? null : req.email().toLowerCase().trim();
        if (email != null && !email.isBlank() && users.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists for " + email);
        }

        Student s = new Student();
        s.setName(req.name().trim());
        s.setPhone(phone);
        s.setEmail(email);
        s.setGender(req.gender());
        s.setDateOfBirth(req.dateOfBirth());
        s.setEducationalQualification(req.educationalQualification());
        s.setState("Karnataka");
        s.setZoneId(emptyToNull(req.zoneId()));
        s.setCenterId(emptyToNull(req.centerId()));
        s.setDistrict(emptyToNull(req.district()));
        s.setTaluk(emptyToNull(req.taluk()));
        s.setGramPanchayat(emptyToNull(req.gramPanchayat()));
        // Prefer email as the login id when given, else the mobile number.
        s.setUserId((email != null && !email.isBlank()) ? email : phone);

        StudentRegistrationResult result = studentRegistration.register(s);
        return new StudentSelfRegister.Result(
                result.registerNo(),
                result.loginId(),
                "Registration successful. Sign in with the OTP sent to your registered contact.");
    }

    /** Request an OTP for a student (by User ID / email or mobile number). */
    public OtpRequestResult requestOtp(String identifier) {
        User user = resolveStudent(identifier)
                .orElseThrow(() -> new BadCredentialsException("No student account found for " + identifier));
        String code = otp.generate(identifier);
        log.info("OTP for student {} = {}", user.getEmail(), code); // never log in production

        String email = user.getEmail();
        boolean delivered = false;
        if (mailSender.isPresent() && StringUtils.hasText(mailFrom) && StringUtils.hasText(email)) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom);
                msg.setTo(email);
                msg.setSubject("MSYEP — your login OTP");
                msg.setText("Your MSYEP login OTP is " + code + ". It is valid for 5 minutes.");
                mailSender.get().send(msg);
                delivered = true;
            } catch (Exception ex) {
                log.warn("OTP email failed for {}: {}", email, ex.getMessage());
            }
        }
        String target = mask(email);
        return delivered
                ? new OtpRequestResult(true, target, "OTP sent to " + target, null)
                : new OtpRequestResult(false, target,
                    "Mail/SMS not configured — OTP shown here for testing.", code);
    }

    /** Verify an OTP and return a JWT for the student. */
    public AuthResponse verifyOtp(String identifier, String code) {
        User user = resolveStudent(identifier)
                .orElseThrow(() -> new BadCredentialsException("No student account found"));
        if (!user.isActive()) throw new BadCredentialsException("Account is disabled");
        if (!otp.verify(identifier, code)) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }
        return toResponse(user);
    }

    /** Resolve a STUDENT login by email/userId or by student mobile number. */
    private Optional<User> resolveStudent(String identifier) {
        String id = identifier.toLowerCase().trim();
        Optional<User> byEmail = users.findByEmail(id).filter(u -> u.getRole() == Role.STUDENT);
        if (byEmail.isPresent()) return byEmail;
        return students.findByPhone(identifier.trim())
                .flatMap(s -> users.findByStudentId(s.getId()))
                .filter(u -> u.getRole() == Role.STUDENT);
    }

    private AuthResponse toResponse(User user) {
        String token = jwt.generate(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.getZoneId(), user.getCenterId(), user.getStudentId());
    }

    private static String emptyToNull(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static String mask(String v) {
        if (!StringUtils.hasText(v)) return "your registered contact";
        int at = v.indexOf('@');
        if (at > 1) return v.charAt(0) + "***" + v.substring(at);
        return v.length() <= 4 ? "****" : v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }
}
